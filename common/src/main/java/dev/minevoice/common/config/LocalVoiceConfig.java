package dev.minevoice.common.config;

public record LocalVoiceConfig(
        String bindHost,
        int bindPort,
        String advertiseHost,
        int advertisePort,
        int maxDistance,
        int fadeStartDistance,
        boolean enableOcclusion,
        boolean enableDebugLog
) implements VoiceConfig {
    @Override
    public VoiceMode mode() {
        return VoiceMode.LOCAL;
    }
}
