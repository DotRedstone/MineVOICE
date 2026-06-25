package dev.minevoice.neoforge.config;

import dev.minevoice.common.config.VoiceConstants;
import dev.minevoice.common.config.VoiceMode;

public record MineVoiceModConfig(
        VoiceMode mode,
        String localVoiceBindHost,
        int localVoiceBindPort,
        String localVoiceAdvertiseHost,
        int localVoiceAdvertisePort,
        String remoteVoiceHost,
        int remoteVoicePort,
        String sharedSecret,
        int proximityDistance,
        boolean enableLanVoiceServer,
        boolean enableSpatialDebug,
        String voiceCodec,
        String audioPlaybackBackend,
        String spatialBackend,
        boolean enableOcclusion,
        double occlusionStrength,
        boolean occlusionLowPass,
        boolean enableSoundPhysicsCompat,
        int jitterBufferMs,
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
                VoiceConstants.DEFAULT_MAX_DISTANCE,
                true,
                false,
                "mock",
                "auto",
                "auto",
                true,
                0.6D,
                true,
                true,
                60,
                false
        );
    }

    public String bindHost() {
        return localVoiceBindHost;
    }

    public int bindPort() {
        return localVoiceBindPort;
    }

    public String advertiseHost() {
        return localVoiceAdvertiseHost;
    }

    public int advertisePort() {
        return localVoiceAdvertisePort;
    }
}
