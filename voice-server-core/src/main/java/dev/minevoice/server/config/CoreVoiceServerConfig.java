package dev.minevoice.server.config;

import dev.minevoice.common.config.VoiceConstants;

public record CoreVoiceServerConfig(
        String bindHost,
        int bindPort,
        String sharedSecret,
        int maxPlayers,
        double proximityDistance,
        boolean enableBandwidthStats,
        boolean enableDebugLog
) {
    public static CoreVoiceServerConfig defaults() {
        return new CoreVoiceServerConfig(
                "0.0.0.0",
                VoiceConstants.DEFAULT_UDP_PORT,
                "change-me",
                100,
                48.0D,
                true,
                false
        );
    }
}
