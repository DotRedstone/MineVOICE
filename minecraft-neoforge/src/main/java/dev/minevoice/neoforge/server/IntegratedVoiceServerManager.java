package dev.minevoice.neoforge.server;

import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.neoforge.config.MineVoiceModConfig;

public final class IntegratedVoiceServerManager {
    private final MineVoiceModConfig config;
    private final MineVoiceLogger logger;
    private boolean running;

    public IntegratedVoiceServerManager(MineVoiceModConfig config) {
        this.config = config;
        this.logger = new MineVoiceLogger("neoforge-integrated", config.enableDebugLog());
    }

    public void start() {
        running = true;
        logger.info("integrated voice server reserved at " + config.bindHost() + ":" + config.bindPort());
        // TODO(minevoice): start the integrated UDP voice server for Local mode.
    }

    public void stop() {
        running = false;
        logger.info("integrated voice server stopped");
    }

    public boolean running() {
        return running;
    }
}
