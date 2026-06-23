package dev.minevoice.standalone.config;

import dev.minevoice.common.config.VoiceConstants;

public record StandaloneConfig(
        String bindHost,
        int bindPort,
        String sharedSecret,
        int maxPlayers,
        double proximityDistance,
        boolean enableBandwidthStats,
        boolean enableDebugLog
) {
    public static StandaloneConfig defaults() {
        return new StandaloneConfig(
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
