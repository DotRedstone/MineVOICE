package dev.minevoice.neoforge.client.audio;

import com.mojang.logging.LogUtils;
import dev.minevoice.common.audio.AudioCaptureProcessingRequest;
import dev.minevoice.common.audio.AudioCaptureProcessor;
import dev.minevoice.common.audio.VoiceActivityGate;
import dev.minevoice.common.audio.VoiceAudioFormat;
import dev.minevoice.common.audio.VoiceCodec;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.neoforge.client.ClientAudioSettings;
import dev.minevoice.neoforge.client.VoiceActivationMode;
import org.slf4j.Logger;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 负责音频采集与编码的工作线程任务。
 * <p>
 * 该类从麦克风读取原始音频数据（PCM），执行音量调节、语音活动检测（VAD），
 * 最终使用 Opus 编码并将生成的 VoiceFrame 通过回调发送。
 */
public class ClientAudioCaptureWorker implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final VoiceAudioFormat CAPTURE_VOICE_FORMAT = VoiceAudioFormat.narrowbandVoice();
    private static final int FRAME_BYTES = 1_920;

    private final UUID playerId;
    private final Supplier<ClientAudioSettings> settingsSupplier;
    private final Consumer<VoiceFrame> frameSender;
    private final VoiceActivityListener activityListener;
    private final AudioCaptureProcessor captureProcessor;
    private final VoiceCodec codec;
    private final AtomicBoolean running;
    
    private final AtomicBoolean proximityPushToTalkDown = new AtomicBoolean();
    private final AtomicBoolean groupPushToTalkDown = new AtomicBoolean();
    private final VoiceActivityGate proximityActivityGate = VoiceActivityGate.defaultVoice();
    private final VoiceActivityGate groupActivityGate = VoiceActivityGate.defaultVoice();
    
    private volatile TargetDataLine activeCaptureLine;
    private long sequence;

    public ClientAudioCaptureWorker(
            UUID playerId,
            Supplier<ClientAudioSettings> settingsSupplier,
            Consumer<VoiceFrame> frameSender,
            VoiceActivityListener activityListener,
            AudioCaptureProcessor captureProcessor,
            VoiceCodec codec,
            AtomicBoolean running
    ) {
        this.playerId = playerId;
        this.settingsSupplier = settingsSupplier;
        this.frameSender = frameSender;
        this.activityListener = activityListener;
        this.captureProcessor = captureProcessor;
        this.codec = codec;
        this.running = running;
    }

    public void setPushToTalkDown(boolean pressed) {
        proximityPushToTalkDown.set(pressed);
    }

    public void setGroupPushToTalkDown(boolean pressed) {
        groupPushToTalkDown.set(pressed);
    }

    public TargetDataLine getActiveCaptureLine() {
        return activeCaptureLine;
    }

    @Override
    public void run() {
        while (running.get()) {
            ClientAudioSettings settings = settingsSupplier.get();
            DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, ClientVoiceAudioPipeline.CAPTURE_FORMAT);
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

    private TargetDataLine openTargetLine(ClientAudioSettings settings, DataLine.Info lineInfo) {
        TargetDataLine line = (TargetDataLine) JavaSoundDeviceSelector.getLine(settings.microphoneDevice(), lineInfo);
        try {
            line.open(ClientVoiceAudioPipeline.CAPTURE_FORMAT, FRAME_BYTES * 4);
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
            if (pushToTalkDown) {
                return activityGate.update(microphoneLevel, 0.01f);
            } else {
                activityGate.reset();
                return false;
            }
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

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
