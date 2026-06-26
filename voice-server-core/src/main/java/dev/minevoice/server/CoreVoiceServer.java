package dev.minevoice.server;

import dev.minevoice.common.config.VoiceConstants;
import dev.minevoice.common.network.BandwidthCounter;
import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.server.auth.CoreTokenValidator;
import dev.minevoice.server.config.CoreVoiceServerConfig;
import dev.minevoice.server.relay.VoiceRelayService;
import dev.minevoice.server.session.VoiceSessionRegistry;
import dev.minevoice.server.metrics.ServerRuntimeStats;
import dev.minevoice.server.udp.UdpVoiceServer;

import dev.minevoice.common.protocol.VoicePlayerState;
import java.util.Collection;

public final class CoreVoiceServer {
    private final CoreVoiceServerConfig config;
    private final MineVoiceLogger logger;
    private final UdpVoiceServer udpVoiceServer;
    private final BandwidthCounter bandwidthCounter = new BandwidthCounter();
    private final VoiceSessionRegistry sessionRegistry = new VoiceSessionRegistry();

    public CoreVoiceServer(CoreVoiceServerConfig config) {
        this.config = config;
        this.logger = new MineVoiceLogger("standalone", config.enableDebugLog());
        this.udpVoiceServer = new UdpVoiceServer(
                config,
                logger,
                new CoreTokenValidator(config.sharedSecret()),
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

    public void replacePlayerStates(Collection<VoicePlayerState> states) {
        sessionRegistry.replacePlayerStates(states);
    }
}
