package dev.minevoice.standalone;

import dev.minevoice.common.config.VoiceConstants;
import dev.minevoice.common.network.BandwidthCounter;
import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.standalone.auth.StandaloneTokenValidator;
import dev.minevoice.standalone.config.StandaloneConfig;
import dev.minevoice.standalone.relay.VoiceRelayService;
import dev.minevoice.standalone.session.VoiceSessionRegistry;
import dev.minevoice.standalone.metrics.ServerRuntimeStats;
import dev.minevoice.standalone.udp.UdpVoiceServer;

public final class StandaloneVoiceServer {
    private final StandaloneConfig config;
    private final MineVoiceLogger logger;
    private final UdpVoiceServer udpVoiceServer;
    private final BandwidthCounter bandwidthCounter = new BandwidthCounter();
    private final VoiceSessionRegistry sessionRegistry = new VoiceSessionRegistry();

    public StandaloneVoiceServer(StandaloneConfig config) {
        this.config = config;
        this.logger = new MineVoiceLogger("standalone", config.enableDebugLog());
        this.udpVoiceServer = new UdpVoiceServer(
                config,
                logger,
                new StandaloneTokenValidator(config.sharedSecret()),
                sessionRegistry,
                new VoiceRelayService(sessionRegistry, config.proximityDistance()),
                bandwidthCounter
        );
    }

    public void start() {
        logger.info(VoiceConstants.DISPLAY_NAME + " " + ServerRuntimeStats.currentVersion());
        logger.info("bind=" + config.bindHost() + ":" + config.bindPort());
        logger.info("maxPlayers=" + config.maxPlayers());
        logger.info("proximityDistance=" + config.proximityDistance());
        logger.info("debug=" + config.enableDebugLog());
        udpVoiceServer.start();
    }

    public void stop() {
        udpVoiceServer.stop();
    }
}
