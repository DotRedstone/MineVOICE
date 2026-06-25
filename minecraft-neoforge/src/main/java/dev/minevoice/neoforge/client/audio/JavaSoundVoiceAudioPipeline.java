package dev.minevoice.neoforge.client.audio;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class JavaSoundVoiceAudioPipeline {
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
            try (TargetDataLine line = openTargetLine(settings)) {
                activeCaptureLine = line;
                line.start();
                byte[] pcm = new byte[FRAME_BYTES];
                while (running.get() && sameCaptureDevice(settings)) {
                    int read = line.read(pcm, 0, pcm.length);
                    float microphoneLevel = read <= 0 ? 0.0F : peakLevel(pcm, read);
                    List<VoiceChannel> channels = read > 0 ? transmitChannels(settings, microphoneLevel) : List.of();
                    boolean transmitting = !channels.isEmpty();
                    activityListener.onActivity(microphoneLevel, transmitting);
                    if (!transmitting) {
                        continue;
                    }
                    byte[] framePcm = Arrays.copyOf(pcm, read);
                    applyVolume(framePcm, settings.microphoneVolume());
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
            try (VoicePlaybackBackend backend = VoicePlaybackBackendFactory.open(settings, PLAYBACK_FORMAT, PLAYBACK_FRAME_BYTES * 8)) {
                activePlaybackBackend = backend;
                backend.start();
                long nextMixAt = System.nanoTime();
                while (running.get() && backend.matches(settingsSupplier.get())) {
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
                        TimeUnit.NANOSECONDS.sleep(waitNanos);
                        continue;
                    }
                    byte[] stereoPcm = mixStereoFrame(sourceFrames, current);
                    if (stereoPcm != null) {
                        backend.writeStereoFrame(stereoPcm, 0, stereoPcm.length);
                    }
                    updatePlaybackStats(sourceFrames, backend.backendName());
                    nextMixAt += FRAME_DURATION_NANOS;
                    if (nextMixAt < now - FRAME_DURATION_NANOS) {
                        nextMixAt = now;
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                sleepQuietly(1_000L);
            } finally {
                activePlaybackBackend = null;
            }
        }
    }

    private TargetDataLine openTargetLine(ClientAudioSettings settings) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, CAPTURE_FORMAT);
        TargetDataLine line = (TargetDataLine) JavaSoundDeviceSelector.getLine(settings.microphoneDevice(), info);
        try {
            line.open(CAPTURE_FORMAT, FRAME_BYTES * 4);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to open microphone line", exception);
        }
        return line;
    }

    private boolean sameCaptureDevice(ClientAudioSettings previous) {
        return previous.microphoneDevice().equals(settingsSupplier.get().microphoneDevice());
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
            activityGate.reset();
            return pushToTalkDown;
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

    private static void applyVolume(byte[] pcm, float volume) {
        float clamped = Math.max(0.0F, Math.min(1.0F, volume));
        for (int index = 0; index + 1 < pcm.length; index += 2) {
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
        Iterator<Map.Entry<UUID, SourcePlaybackState>> iterator = sourceFrames.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SourcePlaybackState> entry = iterator.next();
            SourcePlaybackState source = entry.getValue();
            VoiceFrame frame = source.jitterBuffer().pollReady();
            if (frame == null && !source.jitterBuffer().hasBufferedFrames()) {
                iterator.remove();
            }
            if (frame == null || frame.channels() != 1) {
                continue;
            }
            if (Boolean.TRUE.equals(playerMutedSupplier.apply(frame.senderPlayerId()))) {
                continue;
            }

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
            if (spatializer.occluded(frame.senderPlayerId(), frame.channel())) {
                applyLowPass(pcm, source.lowPassState());
            } else {
                source.lowPassState().reset();
            }
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

    private static void applyLowPass(byte[] pcm, LowPassState state) {
        float previous = state.previousSample();
        for (int index = 0; index + 1 < pcm.length; index += 2) {
            int sample = (pcm[index + 1] << 8) | (pcm[index] & 0xFF);
            float filtered = previous + 0.22F * (sample - previous);
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

    private record SourcePlaybackState(JitterBuffer jitterBuffer, VoiceDecoder decoder, LowPassState lowPassState) {
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
