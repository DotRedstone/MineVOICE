package dev.minevoice.neoforge.config;

import dev.minevoice.common.config.VoiceConstants;
import dev.minevoice.common.config.VoiceMode;

public record MineVoiceModConfig(
        VoiceMode mode,
        String bindHost,
        int bindPort,
        String advertiseHost,
        int advertisePort,
        String remoteVoiceHost,
        int remoteVoicePort,
        String sharedSecret,
        boolean enableDebugLog
) {
    public static MineVoiceModConfig localDefaults() {
        return new MineVoiceModConfig(
                VoiceMode.LOCAL,
                "0.0.0.0",
                VoiceConstants.DEFAULT_UDP_PORT,
                "auto",
                VoiceConstants.DEFAULT_UDP_PORT,
                "",
                VoiceConstants.DEFAULT_UDP_PORT,
                "change-me",
                false
        );
    }
}
