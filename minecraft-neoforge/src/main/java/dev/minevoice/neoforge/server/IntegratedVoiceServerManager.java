package dev.minevoice.neoforge.server;

import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.neoforge.config.MineVoiceModConfig;
import dev.minevoice.standalone.StandaloneVoiceServer;
import dev.minevoice.standalone.config.StandaloneConfig;

import dev.minevoice.common.protocol.VoicePlayerState;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public final class IntegratedVoiceServerManager {
    private final MineVoiceModConfig config;
    private final MineVoiceLogger logger;
    private final AtomicBoolean running = new AtomicBoolean();
    private StandaloneVoiceServer voiceServer;
    private Thread serverThread;

    public IntegratedVoiceServerManager(MineVoiceModConfig config) {
        this.config = config;
        this.logger = new MineVoiceLogger("neoforge-integrated", config.enableDebugLog());
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        StandaloneConfig standaloneConfig = new StandaloneConfig(
                config.localVoiceBindHost(),
                config.localVoiceBindPort(),
                config.sharedSecret(),
                100,
                config.proximityDistance(),
                true,
                config.enableDebugLog()
        );
        voiceServer = new StandaloneVoiceServer(standaloneConfig);
        serverThread = new Thread(() -> {
            try {
                logger.info("MineVOICE local voice server starting on "
                        + config.localVoiceBindHost() + ":" + config.localVoiceBindPort());
                voiceServer.start();
            } catch (RuntimeException exception) {
                if (running.get()) {
                    logger.warn("MineVOICE local voice server failed: " + exception.getMessage());
                }
            } finally {
                running.set(false);
            }
        }, "minevoice-local-udp-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        if (voiceServer != null) {
            voiceServer.stop();
        }
        if (serverThread != null) {
            try {
                serverThread.join(2_000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("MineVOICE local voice server stopped");
    }

    public void replacePlayerStates(Collection<VoicePlayerState> states) {
        if (voiceServer != null) {
            voiceServer.replacePlayerStates(states);
        }
    }

    public boolean running() {
        return running.get();
    }
}
