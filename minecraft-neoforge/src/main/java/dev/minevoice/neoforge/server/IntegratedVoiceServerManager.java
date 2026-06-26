package dev.minevoice.neoforge.server;

import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.neoforge.config.MineVoiceModConfig;
import dev.minevoice.server.CoreVoiceServer;
import dev.minevoice.server.config.CoreVoiceServerConfig;

import dev.minevoice.common.protocol.VoicePlayerState;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public final class IntegratedVoiceServerManager {
    private final MineVoiceModConfig config;
    private final MineVoiceLogger logger;
    private final AtomicBoolean running = new AtomicBoolean();
    private CoreVoiceServer voiceServer;
    private Thread serverThread;
    private volatile String lastError = "";

    public IntegratedVoiceServerManager(MineVoiceModConfig config) {
        this.config = config;
        this.logger = new MineVoiceLogger("neoforge-integrated", config.enableDebugLog());
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        lastError = "";
        CoreVoiceServerConfig standaloneConfig = new CoreVoiceServerConfig(
                config.localVoiceBindHost(),
                config.localVoiceBindPort(),
                config.sharedSecret(),
                100,
                config.proximityDistance(),
                true,
                config.enableDebugLog()
        );
        voiceServer = new CoreVoiceServer(standaloneConfig);
        serverThread = new Thread(() -> {
            try {
                logger.info("MineVOICE local voice server starting on "
                        + config.localVoiceBindHost() + ":" + config.localVoiceBindPort());
                voiceServer.start();
            } catch (RuntimeException exception) {
                if (running.get()) {
                    lastError = exception.getMessage();
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
        boolean wasRunning = running.getAndSet(false);
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
        voiceServer = null;
        serverThread = null;
        if (wasRunning) {
            logger.info("MineVOICE local voice server stopped");
        }
    }

    public void replacePlayerStates(Collection<VoicePlayerState> states) {
        if (voiceServer != null) {
            voiceServer.replacePlayerStates(states);
        }
    }

    public boolean running() {
        return running.get();
    }

    public String lastError() {
        return lastError;
    }
}
