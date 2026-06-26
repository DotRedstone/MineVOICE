package dev.minevoice.neoforge.client.audio;

import com.mojang.logging.LogUtils;
import dev.minevoice.common.audio.AudioCaptureProcessingRequest;
import dev.minevoice.common.audio.AudioCaptureProcessor;
import dev.minevoice.common.audio.JitterBuffer;
import dev.minevoice.common.audio.JitterBufferConfig;
import dev.minevoice.common.audio.JitterBufferStats;
import dev.minevoice.common.audio.NoopAudioCaptureProcessor;
import dev.minevoice.common.audio.VoiceActivityGate;
import dev.minevoice.common.audio.VoiceAudioFormat;
import dev.minevoice.common.audio.VoiceCodec;
import dev.minevoice.common.audio.VoiceCodecFactory;
import dev.minevoice.common.audio.VoiceDecoder;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.VoiceActivationMode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;

public final class JavaSoundVoiceAudioPipeline {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final AudioFormat CAPTURE_FORMAT = new AudioFormat(48_000.0F, 16, 1, true, false);
    public static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(48_000.0F, 16, 2, true, false);
    private static final int FRAME_BYTES = 1_920;
    private static final int SAMPLES_PER_FRAME = FRAME_BYTES / Short.BYTES;
    private static final int PLAYBACK_FRAME_BYTES = SAMPLES_PER_FRAME * Short.BYTES * 2;
    private static final long FRAME_DURATION_NANOS = TimeUnit.MILLISECONDS.toNanos(20L);
    private static final JitterBufferConfig JITTER_BUFFER_CONFIG = JitterBufferConfig.defaultVoice();
    private static final VoiceAudioFormat CAPTURE_VOICE_FORMAT = VoiceAudioFormat.narrowbandVoice();

    private final UUID playerId;
    private final Supplier<ClientAudioSettings> settingsSupplier;
    private final Consumer<VoiceFrame> frameSender;
    private final VoiceActivityListener activityListener;
    private final VoiceSpatializer spatializer;
    private final Function<UUID, Float> playerVolumeSupplier;
    private final Function<UUID, Boolean> playerMutedSupplier;
    private final AudioCaptureProcessor captureProcessor = NoopAudioCaptureProcessor.INSTANCE;
    private final VoiceCodec codec;
    private final BlockingQueue<VoiceFrame> playbackQueue = new LinkedBlockingQueue<>(512);
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean proximityPushToTalkDown = new AtomicBoolean();
    private final AtomicBoolean groupPushToTalkDown = new AtomicBoolean();
    private final VoiceActivityGate proximityActivityGate = VoiceActivityGate.defaultVoice();
    private final VoiceActivityGate groupActivityGate = VoiceActivityGate.defaultVoice();
    private final AtomicReference<VoicePlaybackStats> playbackStats = new AtomicReference<>(VoicePlaybackStats.empty());
    private Thread captureThread;
    private Thread playbackThread;
    private volatile TargetDataLine activeCaptureLine;
    private volatile VoicePlaybackBackend activePlaybackBackend;
    private volatile boolean forceJavaSoundPlayback;
    private long sequence;

    public JavaSoundVoiceAudioPipeline(
            UUID playerId,
            Supplier<ClientAudioSettings> settingsSupplier,
            Consumer<VoiceFrame> frameSender,
            VoiceActivityListener activityListener,
            VoiceSpatializer spatializer,
            Function<UUID, Float> playerVolumeSupplier,
            Function<UUID, Boolean> playerMutedSupplier
    ) {
        this.playerId = playerId;
        this.settingsSupplier = settingsSupplier;
        this.frameSender = frameSender;
        this.activityListener = activityListener;
        this.spatializer = spatializer;
        this.playerVolumeSupplier = playerVolumeSupplier;
        this.playerMutedSupplier = playerMutedSupplier;
        this.codec = VoiceCodecFactory.create(settingsSupplier.get().voiceCodec());
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        captureThread = new Thread(this::captureLoop, "minevoice-audio-capture");
        playbackThread = new Thread(this::playbackLoop, "minevoice-audio-playback");
        captureThread.setDaemon(true);
        playbackThread.setDaemon(true);
        captureThread.start();
        playbackThread.start();
    }

    public void stop() {
        running.set(false);
        if (captureThread != null) {
            captureThread.interrupt();
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
        closeLine(activeCaptureLine);
        closePlaybackBackend(activePlaybackBackend);
        playbackQueue.clear();
        playbackStats.set(VoicePlaybackStats.empty());
    }

    public void setPushToTalkDown(boolean pressed) {
        proximityPushToTalkDown.set(pressed);
    }

    public void setGroupPushToTalkDown(boolean pressed) {
        groupPushToTalkDown.set(pressed);
    }

    public void enqueuePlayback(VoiceFrame frame) {
        playbackQueue.offer(frame);
    }

    public VoicePlaybackStats playbackStats() {
        return playbackStats.get();
    }

    private void captureLoop() {
        while (running.get()) {
            ClientAudioSettings settings = settingsSupplier.get();
            DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT);
            String lineSignature = JavaSoundDeviceSelector.lineSignature(settings.microphoneDevice(), lineInfo);
            try (TargetDataLine line = openTargetLine(settings, lineInfo)) {
                activeCaptureLine = line;
                line.start();
                byte[] pcm = new byte[FRAME_BYTES];
                java.util.ArrayDeque<byte[]> preRecordBuffer = new java.util.ArrayDeque<>(10);
                while (running.get() && sameCaptureDevice(settings, lineInfo, lineSignature)) {
                    int read = line.read(pcm, 0, pcm.length);
                    if (read > 0) {
                        applyVolume(pcm, read, settings.microphoneVolume());
                    }
                    float microphoneLevel = read <= 0 ? 0.0F : peakLevel(pcm, read);
                    List<VoiceChannel> channels = read > 0 ? transmitChannels(settings, microphoneLevel) : List.of();
                    boolean transmitting = !channels.isEmpty();
                    activityListener.onActivity(microphoneLevel, transmitting);
                    
                    if (!transmitting) {
                        if (read > 0) {
                            preRecordBuffer.addLast(Arrays.copyOf(pcm, read));
                            if (preRecordBuffer.size() > 10) {
                                preRecordBuffer.removeFirst();
                            }
                        }
                        continue;
                    }
                    
                    preRecordBuffer.addLast(Arrays.copyOf(pcm, read));
                    while (!preRecordBuffer.isEmpty()) {
                        byte[] framePcm = preRecordBuffer.removeFirst();
                        long timestamp = System.currentTimeMillis();
                        framePcm = captureProcessor.process(new AudioCaptureProcessingRequest(
                                framePcm,
                                CAPTURE_VOICE_FORMAT,
                                microphoneLevel,
                                timestamp
                        ));
                        byte[] encoded = codec.encode(framePcm);
                        for (VoiceChannel channel : channels) {
                            frameSender.accept(new VoiceFrame(playerId, ++sequence, timestamp, encoded, 48_000, 1, channel));
                        }
                    }
                }
            } catch (RuntimeException exception) {
                sleepQuietly(1_000L);
            } finally {
                activeCaptureLine = null;
            }
        }
    }

    private void playbackLoop() {
        Map<UUID, SourcePlaybackState> sourceFrames = new HashMap<>();
        while (running.get()) {
            ClientAudioSettings settings = settingsSupplier.get();
            String requestedBackend = VoicePlaybackBackendFactory.normalizeBackendName(settings.audioPlaybackBackend());
            if (!OpenAlVoicePlaybackBackend.NAME.equals(requestedBackend)) {
                forceJavaSoundPlayback = false;
            }
            ClientAudioSettings backendSettings = forceJavaSoundPlayback
                    ? settings.withAudioPlaybackBackend(JavaSoundVoicePlaybackBackend.NAME)
                    : settings;
            try (VoicePlaybackBackend backend = VoicePlaybackBackendFactory.open(backendSettings, PLAYBACK_FORMAT, PLAYBACK_FRAME_BYTES * 8)) {
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
                    LOGGER.warn("MineVOICE OpenAL playback failed; falling back to Java Sound", exception);
                }
                sleepQuietly(1_000L);
            } finally {
                activePlaybackBackend = null;
            }
        }
    }

    private TargetDataLine openTargetLine(ClientAudioSettings settings, DataLine.Info lineInfo) {
        TargetDataLine line = (TargetDataLine) JavaSoundDeviceSelector.getLine(settings.microphoneDevice(), lineInfo);
        try {
            line.open(CAPTURE_FORMAT, FRAME_BYTES * 4);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to open microphone line", exception);
        }
        return line;
    }

    private boolean sameCaptureDevice(ClientAudioSettings previous, DataLine.Info lineInfo, String lineSignature) {
        ClientAudioSettings current = settingsSupplier.get();
        return previous.microphoneDevice().equals(current.microphoneDevice())
                && lineSignature.equals(JavaSoundDeviceSelector.lineSignature(current.microphoneDevice(), lineInfo));
    }

    private ClientAudioSettings backendSettingsForCurrentPlayback() {
        ClientAudioSettings current = settingsSupplier.get();
        return forceJavaSoundPlayback && OpenAlVoicePlaybackBackend.NAME.equals(VoicePlaybackBackendFactory.normalizeBackendName(current.audioPlaybackBackend()))
                ? current.withAudioPlaybackBackend(JavaSoundVoicePlaybackBackend.NAME)
                : current;
    }

    private List<VoiceChannel> transmitChannels(ClientAudioSettings settings, float microphoneLevel) {
        if (settings.muted()) {
            return List.of();
        }
        boolean proximityActive = shouldTransmit(
                settings.activationMode(),
                proximityPushToTalkDown.get(),
                microphoneLevel,
                settings.voiceActivationThreshold(),
                proximityActivityGate
        );
        boolean groupActive = shouldTransmit(
                settings.groupActivationMode(),
                groupPushToTalkDown.get(),
                microphoneLevel,
                settings.groupVoiceActivationThreshold(),
                groupActivityGate
        );
        if (proximityActive && groupActive) {
            return List.of(VoiceChannel.PROXIMITY, VoiceChannel.GROUP);
        }
        if (proximityActive) {
            return List.of(VoiceChannel.PROXIMITY);
        }
        return groupActive ? List.of(VoiceChannel.GROUP) : List.of();
    }

    private static boolean shouldTransmit(
            VoiceActivationMode activationMode,
            boolean pushToTalkDown,
            float microphoneLevel,
            float threshold,
            VoiceActivityGate activityGate
    ) {
        if (activationMode == VoiceActivationMode.PUSH_TO_TALK) {
            if (!pushToTalkDown) {
                activityGate.reset();
                return false;
            }
            return activityGate.update(microphoneLevel, 0.01f);
        }
        return activityGate.update(microphoneLevel, threshold);
    }

    private static float peakLevel(byte[] pcm, int bytes) {
        int peak = 0;
        for (int index = 0; index + 1 < bytes; index += 2) {
            int sample = (pcm[index + 1] << 8) | (pcm[index] & 0xFF);
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak / 32768.0F;
    }

    private static void applyVolume(byte[] pcm, int length, float volume) {
        float clamped = Math.max(0.0F, Math.min(2.0F, volume));
        if (clamped == 1.0F) {
            return;
        }
        for (int index = 0; index + 1 < length; index += 2) {
            int sample = (pcm[index + 1] << 8) | (pcm[index] & 0xFF);
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * clamped)));
            pcm[index] = (byte) (scaled & 0xFF);
            pcm[index + 1] = (byte) ((scaled >>> 8) & 0xFF);
        }
    }

    private static void closeLine(DataLine line) {
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    private static void closePlaybackBackend(VoicePlaybackBackend backend) {
        if (backend != null) {
            backend.close();
        }
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
        float smoothing = 0.08F + 0.84F * clampedHighFrequency;
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

    private static final class LowPassState {
        private float previousSample;

        float previousSample() {
            return previousSample;
        }

        void setPreviousSample(float previousSample) {
            this.previousSample = previousSample;
        }

        void reset() {
            previousSample = 0.0F;
        }
    }
}
