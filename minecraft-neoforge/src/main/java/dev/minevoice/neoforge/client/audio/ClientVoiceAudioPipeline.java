package dev.minevoice.neoforge.client.audio;

import com.mojang.logging.LogUtils;
import dev.minevoice.common.audio.AudioCaptureProcessor;
import dev.minevoice.common.audio.NoopAudioCaptureProcessor;
import dev.minevoice.common.audio.VoiceCodec;
import dev.minevoice.common.audio.VoiceCodecFactory;
import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.neoforge.client.ClientAudioSettings;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 客户端语音音频流水线协调器 (Coordinator)
 * <p>
 * 该类负责初始化并管理两个独立的后台工作线程：
 * 1. {@link ClientAudioCaptureWorker}：处理麦克风音频录制、VAD、编码并发出网络包。
 * 2. {@link ClientAudioPlaybackWorker}：处理网络接收的音频数据、Jitter Buffer 缓冲、解码和空间音频混合。
 * <p>
 * 通过将职责解耦，此协调器仅关注生命周期管理、线程调度及状态汇聚。
 */
public final class ClientVoiceAudioPipeline {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 全局通用音频格式常量
    public static final AudioFormat CAPTURE_FORMAT = new AudioFormat(48_000.0F, 16, 1, true, false);
    public static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(48_000.0F, 16, 2, true, false);

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
    private final AtomicReference<VoicePlaybackStats> playbackStats = new AtomicReference<>(VoicePlaybackStats.empty());

    private Thread captureThread;
    private Thread playbackThread;

    private ClientAudioCaptureWorker captureWorker;
    private ClientAudioPlaybackWorker playbackWorker;

    public ClientVoiceAudioPipeline(
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

        // 初始化子工作器
        this.captureWorker = new ClientAudioCaptureWorker(
                playerId, settingsSupplier, frameSender, activityListener, captureProcessor, codec, running
        );

        this.playbackWorker = new ClientAudioPlaybackWorker(
                settingsSupplier, spatializer, playerVolumeSupplier, playerMutedSupplier, codec, playbackQueue, running, playbackStats,
                (forceJavaSound) -> {
                    // JavaSound 降级回掉，不需要在这里额外处理，PlaybackWorker 内部自我管理
                }
        );

        captureThread = new Thread(captureWorker, "minevoice-audio-capture");
        playbackThread = new Thread(playbackWorker, "minevoice-audio-playback");
        
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

        if (captureWorker != null && captureWorker.getActiveCaptureLine() != null) {
            try {
                captureWorker.getActiveCaptureLine().stop();
                captureWorker.getActiveCaptureLine().close();
            } catch (Exception ignored) { }
        }

        if (playbackWorker != null && playbackWorker.getActivePlaybackBackend() != null) {
            try {
                playbackWorker.getActivePlaybackBackend().close();
            } catch (Exception ignored) { }
        }

        playbackQueue.clear();
        playbackStats.set(VoicePlaybackStats.empty());
    }

    public void setPushToTalkDown(boolean pressed) {
        if (captureWorker != null) {
            captureWorker.setPushToTalkDown(pressed);
        }
    }

    public void setGroupPushToTalkDown(boolean pressed) {
        if (captureWorker != null) {
            captureWorker.setGroupPushToTalkDown(pressed);
        }
    }

    public void enqueuePlayback(VoiceFrame frame) {
        playbackQueue.offer(frame);
    }

    public VoicePlaybackStats playbackStats() {
        return playbackStats.get();
    }
}
