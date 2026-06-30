package dev.minevoice.neoforge.client.audio;

import com.mojang.logging.LogUtils;
import dev.minevoice.common.audio.JitterBuffer;
import dev.minevoice.common.audio.JitterBufferConfig;
import dev.minevoice.common.audio.JitterBufferStats;
import dev.minevoice.common.audio.VoiceCodec;
import dev.minevoice.common.audio.VoiceDecoder;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.neoforge.client.ClientAudioSettings;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 负责音频接收、抗抖动缓冲（Jitter Buffer）、Opus 解码以及空间音频混合播放的工作线程任务。
 * <p>
 * 该类接收通过网络传输过来的 VoiceFrame，将它们放入对应声源的缓冲区，进行解码。
 * 如果使用 OpenAL，会将解码后的 PCM 和声学参数直接抛给 OpenAL 进行硬件级 3D 渲染；
 * 如果使用 JavaSound（降级），则会在软件层面计算衰减并将左右声道混音后输出。
 */
public class ClientAudioPlaybackWorker implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int FRAME_BYTES = 1_920;
    private static final int SAMPLES_PER_FRAME = FRAME_BYTES / Short.BYTES;
    private static final int PLAYBACK_FRAME_BYTES = SAMPLES_PER_FRAME * Short.BYTES * 2;
    private static final long FRAME_DURATION_NANOS = TimeUnit.MILLISECONDS.toNanos(20L);
    private static final JitterBufferConfig JITTER_BUFFER_CONFIG = JitterBufferConfig.defaultVoice();

    private final Supplier<ClientAudioSettings> settingsSupplier;
    private final VoiceSpatializer spatializer;
    private final Function<UUID, Float> playerVolumeSupplier;
    private final Function<UUID, Boolean> playerMutedSupplier;
    private final VoiceCodec codec;
    private final BlockingQueue<VoiceFrame> playbackQueue;
    private final AtomicBoolean running;
    private final AtomicReference<VoicePlaybackStats> playbackStats;
    private final Consumer<Boolean> javaSoundFallbackCallback;

    private volatile VoicePlaybackBackend activePlaybackBackend;
    private volatile boolean forceJavaSoundPlayback;

    public ClientAudioPlaybackWorker(
            Supplier<ClientAudioSettings> settingsSupplier,
            VoiceSpatializer spatializer,
            Function<UUID, Float> playerVolumeSupplier,
            Function<UUID, Boolean> playerMutedSupplier,
            VoiceCodec codec,
            BlockingQueue<VoiceFrame> playbackQueue,
            AtomicBoolean running,
            AtomicReference<VoicePlaybackStats> playbackStats,
            Consumer<Boolean> javaSoundFallbackCallback
    ) {
        this.settingsSupplier = settingsSupplier;
        this.spatializer = spatializer;
        this.playerVolumeSupplier = playerVolumeSupplier;
        this.playerMutedSupplier = playerMutedSupplier;
        this.codec = codec;
        this.playbackQueue = playbackQueue;
        this.running = running;
        this.playbackStats = playbackStats;
        this.javaSoundFallbackCallback = javaSoundFallbackCallback;
    }

    public VoicePlaybackBackend getActivePlaybackBackend() {
        return activePlaybackBackend;
    }

    public boolean isForceJavaSoundPlayback() {
        return forceJavaSoundPlayback;
    }

    public void setForceJavaSoundPlayback(boolean force) {
        this.forceJavaSoundPlayback = force;
    }

    @Override
    public void run() {
        Map<UUID, SourcePlaybackState> sourceFrames = new HashMap<>();
        while (running.get()) {
            ClientAudioSettings settings = settingsSupplier.get();
            String requestedBackend = VoicePlaybackBackendFactory.normalizeBackendName(settings.audioPlaybackBackend());
            if (!OpenAlVoicePlaybackBackend.NAME.equals(requestedBackend)) {
                forceJavaSoundPlayback = false;
                javaSoundFallbackCallback.accept(false);
            }
            ClientAudioSettings backendSettings = forceJavaSoundPlayback
                    ? settings.withAudioPlaybackBackend(JavaSoundVoicePlaybackBackend.NAME)
                    : settings;
            try (VoicePlaybackBackend backend = VoicePlaybackBackendFactory.open(backendSettings, ClientVoiceAudioPipeline.PLAYBACK_FORMAT, PLAYBACK_FRAME_BYTES * 8)) {
                activePlaybackBackend = backend;
                backend.start();
                long nextMixAt = System.nanoTime();
                while (running.get() && backend.matches(backendSettingsForCurrentPlayback())) {
                    drainPlaybackQueue(sourceFrames);
                    updatePlaybackStats(sourceFrames, backend.backendName());
                    ClientAudioSettings current = settingsSupplier.get();
                    if (current.deafened()) {
                        sourceFrames.clear();
                        playbackQueue.clear();
                        playbackStats.set(VoicePlaybackStats.empty());
                        continue;
                    }
                    if (sourceFrames.isEmpty()) {
                        VoiceFrame frame = playbackQueue.poll(20L, TimeUnit.MILLISECONDS);
                        if (frame != null) {
                            enqueueSourceFrame(sourceFrames, frame);
                        }
                        nextMixAt = System.nanoTime();
                        continue;
                    }

                    long now = System.nanoTime();
                    long waitNanos = nextMixAt - now;
                    if (waitNanos > 0L) {
                        java.util.concurrent.locks.LockSupport.parkNanos(waitNanos);
                    }
                    if (backend.supportsSourcePlayback()) {
                        playSourceFrames(sourceFrames, current, backend);
                    } else {
                        byte[] stereoPcm = mixStereoFrame(sourceFrames, current);
                        if (stereoPcm != null) {
                            backend.writeStereoFrame(stereoPcm, 0, stereoPcm.length);
                        }
                    }
                    updatePlaybackStats(sourceFrames, backend.backendName());
                    nextMixAt += FRAME_DURATION_NANOS;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                if (OpenAlVoicePlaybackBackend.NAME.equals(requestedBackend)) {
                    forceJavaSoundPlayback = true;
                    javaSoundFallbackCallback.accept(true);
                    LOGGER.warn("MineVOICE OpenAL playback failed; falling back to Java Sound", exception);
                }
                sleepQuietly(1_000L);
            } finally {
                activePlaybackBackend = null;
            }
        }
    }

    private ClientAudioSettings backendSettingsForCurrentPlayback() {
        ClientAudioSettings current = settingsSupplier.get();
        return forceJavaSoundPlayback && OpenAlVoicePlaybackBackend.NAME.equals(VoicePlaybackBackendFactory.normalizeBackendName(current.audioPlaybackBackend()))
                ? current.withAudioPlaybackBackend(JavaSoundVoicePlaybackBackend.NAME)
                : current;
    }

    private void drainPlaybackQueue(Map<UUID, SourcePlaybackState> sourceFrames) {
        VoiceFrame frame;
        while ((frame = playbackQueue.poll()) != null) {
            enqueueSourceFrame(sourceFrames, frame);
        }
    }

    private void enqueueSourceFrame(Map<UUID, SourcePlaybackState> sourceFrames, VoiceFrame frame) {
        SourcePlaybackState source = sourceFrames.computeIfAbsent(frame.senderPlayerId(), ignored -> new SourcePlaybackState(
                new JitterBuffer(JITTER_BUFFER_CONFIG),
                codec.createDecoder(),
                new LowPassState()
        ));
        source.jitterBuffer().offer(frame);
    }

    private void updatePlaybackStats(Map<UUID, SourcePlaybackState> sourceFrames, String backendName) {
        int bufferedFrames = 0;
        long latePackets = 0L;
        long droppedPackets = 0L;
        long missingFrames = 0L;
        for (SourcePlaybackState source : sourceFrames.values()) {
            JitterBufferStats stats = source.jitterBuffer().stats();
            bufferedFrames += stats.bufferedFrames();
            latePackets += stats.latePackets();
            droppedPackets += stats.droppedPackets();
            missingFrames += stats.missingFrames();
        }
        playbackStats.set(new VoicePlaybackStats(
                backendName,
                sourceFrames.size(),
                bufferedFrames,
                latePackets,
                droppedPackets,
                missingFrames
        ));
    }

    private byte[] mixStereoFrame(Map<UUID, SourcePlaybackState> sourceFrames, ClientAudioSettings settings) {
        int[] left = new int[SAMPLES_PER_FRAME];
        int[] right = new int[SAMPLES_PER_FRAME];
        boolean mixedAudio = false;
        float volume = clampVolume(settings.masterVolume() * settings.voiceChatVolume());
        VoiceSpatialSceneProvider sceneProvider = spatializer instanceof VoiceSpatialSceneProvider provider ? provider : null;
        Iterator<Map.Entry<UUID, SourcePlaybackState>> iterator = sourceFrames.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SourcePlaybackState> entry = iterator.next();
            SourcePlaybackState source = entry.getValue();
            VoiceFrame frame = source.jitterBuffer().pollReady();
            if (frame != null) {
                source.markActive();
            }
            if (frame == null && !source.jitterBuffer().hasBufferedFrames() && System.currentTimeMillis() - source.lastActivity() > 1000L) {
                iterator.remove();
            }
            if (frame == null || frame.channels() != 1) {
                continue;
            }
            if (Boolean.TRUE.equals(playerMutedSupplier.apply(frame.senderPlayerId()))) {
                continue;
            }

            VoiceSourceSnapshot sourceSnapshot = sceneProvider == null || !settings.spatialAudioEnabled()
                    ? VoiceSourceSnapshot.unknown(frame.senderPlayerId())
                    : sceneProvider.sourceSnapshot(frame.senderPlayerId());
            StereoGains gains = settings.spatialAudioEnabled()
                    ? spatializer.gainsFor(frame.senderPlayerId(), frame.channel())
                    : StereoGains.CENTER;
            float playerVolume = clampPlayerVolume(playerVolumeSupplier.apply(frame.senderPlayerId()));
            float leftGain = volume * playerVolume * gains.left();
            float rightGain = volume * playerVolume * gains.right();
            if (leftGain <= 0.0F && rightGain <= 0.0F) {
                continue;
            }
            byte[] pcm = source.decoder().decode(frame.encodedAudio());
            applyLowPass(pcm, source.lowPassState(), sourceSnapshot.highFrequencyGain());
            int sampleCount = Math.min(SAMPLES_PER_FRAME, pcm.length / Short.BYTES);
            for (int index = 0; index < sampleCount; index++) {
                int offset = index * Short.BYTES;
                int sample = (pcm[offset + 1] << 8) | (pcm[offset] & 0xFF);
                left[index] += Math.round(sample * leftGain);
                right[index] += Math.round(sample * rightGain);
            }
            mixedAudio = mixedAudio || sampleCount > 0;
        }
        if (!mixedAudio) {
            return null;
        }

        byte[] stereo = new byte[PLAYBACK_FRAME_BYTES];
        for (int index = 0; index < SAMPLES_PER_FRAME; index++) {
            writeSample(stereo, index * 4, left[index]);
            writeSample(stereo, index * 4 + 2, right[index]);
        }
        return stereo;
    }

    private void playSourceFrames(
            Map<UUID, SourcePlaybackState> sourceFrames,
            ClientAudioSettings settings,
            VoicePlaybackBackend backend
    ) {
        VoiceSpatialSceneProvider sceneProvider = spatializer instanceof VoiceSpatialSceneProvider provider ? provider : null;
        if (sceneProvider != null) {
            backend.updateListener(sceneProvider.listenerSnapshot());
        }
        float volume = clampVolume(settings.masterVolume() * settings.voiceChatVolume());
        Iterator<Map.Entry<UUID, SourcePlaybackState>> iterator = sourceFrames.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SourcePlaybackState> entry = iterator.next();
            SourcePlaybackState source = entry.getValue();
            VoiceFrame frame;
            boolean hasFrames = false;
            while ((frame = source.jitterBuffer().pollReady()) != null) {
                source.markActive();
                hasFrames = true;
                if (frame.channels() != 1) {
                    continue;
                }
                if (Boolean.TRUE.equals(playerMutedSupplier.apply(frame.senderPlayerId()))) {
                    continue;
                }

                VoiceSourceSnapshot sourceSnapshot = sceneProvider == null || !settings.spatialAudioEnabled()
                        ? VoiceSourceSnapshot.unknown(frame.senderPlayerId())
                        : sceneProvider.sourceSnapshot(frame.senderPlayerId());
                float spatialGain = 1.0F;
                if (settings.spatialAudioEnabled() && frame.channel() == VoiceChannel.PROXIMITY) {
                    spatializer.gainsFor(frame.senderPlayerId(), frame.channel());
                    spatialGain = sourceSnapshot.directGain();
                }
                float playerVolume = clampPlayerVolume(playerVolumeSupplier.apply(frame.senderPlayerId()));
                float gain = volume * playerVolume * spatialGain;
                if (gain <= 0.0F) {
                    continue;
                }
                byte[] pcm = source.decoder().decode(frame.encodedAudio());
                backend.writeSourceFrame(
                        frame.senderPlayerId(),
                        frame.channel(),
                        sourceSnapshot,
                        pcm,
                        frame.sampleRate(),
                        gain
                );
            }
            if (!hasFrames && !source.jitterBuffer().hasBufferedFrames() && System.currentTimeMillis() - source.lastActivity() > 1000L) {
                iterator.remove();
            }
        }
        Set<UUID> retainedSources = new HashSet<>(sourceFrames.keySet());
        backend.retainSources(retainedSources);
    }

    private static float clampVolume(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float clampPlayerVolume(Float value) {
        if (value == null) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(2.0F, value));
    }

    private static void writeSample(byte[] target, int offset, int sample) {
        int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
        target[offset] = (byte) (clamped & 0xFF);
        target[offset + 1] = (byte) ((clamped >>> 8) & 0xFF);
    }

    private static void applyLowPass(byte[] pcm, LowPassState state, float highFrequencyGain) {
        float clampedHighFrequency = Math.max(0.02F, Math.min(1.0F, highFrequencyGain));
        float smoothing = 0.02F + 0.98F * clampedHighFrequency;
        float previous = state.previousSample();
        for (int index = 0; index + 1 < pcm.length; index += 2) {
            int sample = (pcm[index + 1] << 8) | (pcm[index] & 0xFF);
            float filtered = previous + smoothing * (sample - previous);
            int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(filtered)));
            pcm[index] = (byte) (clamped & 0xFF);
            pcm[index + 1] = (byte) ((clamped >>> 8) & 0xFF);
            previous = filtered;
        }
        state.setPreviousSample(previous);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 内部状态类：保存单个声源的抗抖动缓冲区和解码器。
     */
    private static final class SourcePlaybackState {
        private final JitterBuffer jitterBuffer;
        private final VoiceDecoder decoder;
        private final LowPassState lowPassState;
        private long lastActivity = System.currentTimeMillis();

        private SourcePlaybackState(JitterBuffer jitterBuffer, VoiceDecoder decoder, LowPassState lowPassState) {
            this.jitterBuffer = jitterBuffer;
            this.decoder = decoder;
            this.lowPassState = lowPassState;
        }

        public JitterBuffer jitterBuffer() { return jitterBuffer; }
        public VoiceDecoder decoder() { return decoder; }
        public LowPassState lowPassState() { return lowPassState; }
        public long lastActivity() { return lastActivity; }
        public void markActive() { lastActivity = System.currentTimeMillis(); }
    }

    /**
     * 内部状态类：保存低通滤波器的上一采样值，用于平滑波形计算。
     */
    private static final class LowPassState {
        private float previousSample;
        float previousSample() { return previousSample; }
        void setPreviousSample(float previousSample) { this.previousSample = previousSample; }
        void reset() { previousSample = 0.0F; }
    }

    public interface Consumer<T> {
        void accept(T t);
    }
}
