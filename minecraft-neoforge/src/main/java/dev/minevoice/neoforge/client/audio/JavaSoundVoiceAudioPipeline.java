package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.audio.MockVoiceCodec;
import dev.minevoice.common.audio.VoiceCodec;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.VoiceActivationMode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class JavaSoundVoiceAudioPipeline {
    public static final AudioFormat CAPTURE_FORMAT = new AudioFormat(48_000.0F, 16, 1, true, false);
    public static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(48_000.0F, 16, 2, true, false);
    private static final int FRAME_BYTES = 1_920;
    private static final int SAMPLES_PER_FRAME = FRAME_BYTES / Short.BYTES;
    private static final int PLAYBACK_FRAME_BYTES = SAMPLES_PER_FRAME * Short.BYTES * 2;
    private static final long FRAME_DURATION_NANOS = TimeUnit.MILLISECONDS.toNanos(20L);
    private static final int MAX_QUEUED_FRAMES_PER_SPEAKER = 8;

    private final UUID playerId;
    private final Supplier<ClientAudioSettings> settingsSupplier;
    private final Consumer<VoiceFrame> frameSender;
    private final VoiceActivityListener activityListener;
    private final VoiceSpatializer spatializer;
    private final VoiceCodec codec = new MockVoiceCodec();
    private final BlockingQueue<VoiceFrame> playbackQueue = new LinkedBlockingQueue<>(512);
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean proximityPushToTalkDown = new AtomicBoolean();
    private final AtomicBoolean groupPushToTalkDown = new AtomicBoolean();
    private Thread captureThread;
    private Thread playbackThread;
    private long sequence;

    public JavaSoundVoiceAudioPipeline(
            UUID playerId,
            Supplier<ClientAudioSettings> settingsSupplier,
            Consumer<VoiceFrame> frameSender,
            VoiceActivityListener activityListener,
            VoiceSpatializer spatializer
    ) {
        this.playerId = playerId;
        this.settingsSupplier = settingsSupplier;
        this.frameSender = frameSender;
        this.activityListener = activityListener;
        this.spatializer = spatializer;
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
        playbackQueue.clear();
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

    private void captureLoop() {
        while (running.get()) {
            ClientAudioSettings settings = settingsSupplier.get();
            try (TargetDataLine line = openTargetLine(settings)) {
                line.start();
                byte[] pcm = new byte[FRAME_BYTES];
                while (running.get() && sameCaptureDevice(settings)) {
                    int read = line.read(pcm, 0, pcm.length);
                    float microphoneLevel = read <= 0 ? 0.0F : peakLevel(pcm, read);
                    VoiceChannel channel = read > 0 ? transmitChannel(settings, microphoneLevel) : null;
                    boolean transmitting = channel != null;
                    activityListener.onActivity(microphoneLevel, transmitting);
                    if (!transmitting) {
                        continue;
                    }
                    byte[] framePcm = Arrays.copyOf(pcm, read);
                    applyVolume(framePcm, settings.microphoneVolume());
                    byte[] encoded = codec.encode(framePcm);
                    frameSender.accept(new VoiceFrame(playerId, ++sequence, System.currentTimeMillis(), encoded, 48_000, 1, channel));
                }
            } catch (RuntimeException exception) {
                sleepQuietly(1_000L);
            }
        }
    }

    private void playbackLoop() {
        Map<UUID, ArrayDeque<VoiceFrame>> sourceFrames = new HashMap<>();
        while (running.get()) {
            ClientAudioSettings settings = settingsSupplier.get();
            try (SourceDataLine line = openSourceLine(settings)) {
                line.start();
                long nextMixAt = System.nanoTime();
                while (running.get() && samePlaybackDevice(settings)) {
                    drainPlaybackQueue(sourceFrames);
                    ClientAudioSettings current = settingsSupplier.get();
                    if (current.deafened()) {
                        sourceFrames.clear();
                        playbackQueue.clear();
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
                        line.write(stereoPcm, 0, stereoPcm.length);
                    }
                    nextMixAt += FRAME_DURATION_NANOS;
                    if (nextMixAt < now - FRAME_DURATION_NANOS) {
                        nextMixAt = now;
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                sleepQuietly(1_000L);
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

    private SourceDataLine openSourceLine(ClientAudioSettings settings) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, PLAYBACK_FORMAT);
        SourceDataLine line = (SourceDataLine) JavaSoundDeviceSelector.getLine(settings.outputDevice(), info);
        try {
            line.open(PLAYBACK_FORMAT, PLAYBACK_FRAME_BYTES * 8);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to open output line", exception);
        }
        return line;
    }

    private boolean sameCaptureDevice(ClientAudioSettings previous) {
        return previous.microphoneDevice().equals(settingsSupplier.get().microphoneDevice());
    }

    private boolean samePlaybackDevice(ClientAudioSettings previous) {
        return previous.outputDevice().equals(settingsSupplier.get().outputDevice());
    }

    private VoiceChannel transmitChannel(ClientAudioSettings settings, float microphoneLevel) {
        if (settings.muted()) {
            return null;
        }
        if (groupPushToTalkDown.get()) {
            return VoiceChannel.GROUP;
        }
        if (settings.activationMode() == VoiceActivationMode.PUSH_TO_TALK) {
            return proximityPushToTalkDown.get() ? VoiceChannel.PROXIMITY : null;
        }
        return microphoneLevel >= settings.voiceActivationThreshold() ? VoiceChannel.PROXIMITY : null;
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

    private void drainPlaybackQueue(Map<UUID, ArrayDeque<VoiceFrame>> sourceFrames) {
        VoiceFrame frame;
        while ((frame = playbackQueue.poll()) != null) {
            enqueueSourceFrame(sourceFrames, frame);
        }
    }

    private static void enqueueSourceFrame(Map<UUID, ArrayDeque<VoiceFrame>> sourceFrames, VoiceFrame frame) {
        ArrayDeque<VoiceFrame> frames = sourceFrames.computeIfAbsent(frame.senderPlayerId(), ignored -> new ArrayDeque<>());
        if (frames.size() >= MAX_QUEUED_FRAMES_PER_SPEAKER) {
            frames.removeFirst();
        }
        frames.addLast(frame);
    }

    private byte[] mixStereoFrame(Map<UUID, ArrayDeque<VoiceFrame>> sourceFrames, ClientAudioSettings settings) {
        int[] left = new int[SAMPLES_PER_FRAME];
        int[] right = new int[SAMPLES_PER_FRAME];
        boolean mixedAudio = false;
        float volume = clampVolume(settings.masterVolume() * settings.voiceChatVolume());
        Iterator<Map.Entry<UUID, ArrayDeque<VoiceFrame>>> iterator = sourceFrames.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArrayDeque<VoiceFrame>> entry = iterator.next();
            VoiceFrame frame = entry.getValue().pollFirst();
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
            if (frame == null || frame.channels() != 1) {
                continue;
            }

            StereoGains gains = settings.spatialAudioEnabled()
                    ? spatializer.gainsFor(frame.senderPlayerId(), frame.channel())
                    : StereoGains.CENTER;
            float leftGain = volume * gains.left();
            float rightGain = volume * gains.right();
            if (leftGain <= 0.0F && rightGain <= 0.0F) {
                continue;
            }
            byte[] pcm = codec.decode(frame.encodedAudio());
            int sampleCount = Math.min(SAMPLES_PER_FRAME, pcm.length / Short.BYTES);
            for (int index = 0; index < sampleCount; index++) {
                int offset = index * Short.BYTES;
                int sample = (pcm[offset + 1] << 8) | (pcm[offset] & 0xFF);
                left[index] += Math.round(sample * leftGain);
                right[index] += Math.round(sample * rightGain);
            }
            mixedAudio = sampleCount > 0;
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

    private static void writeSample(byte[] target, int offset, int sample) {
        int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
        target[offset] = (byte) (clamped & 0xFF);
        target[offset + 1] = (byte) ((clamped >>> 8) & 0xFF);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
