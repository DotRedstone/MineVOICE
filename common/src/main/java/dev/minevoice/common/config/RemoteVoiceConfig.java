package dev.minevoice.common.config;

public record RemoteVoiceConfig(
        String voiceHost,
        int voicePort,
        String sharedSecret,
        int maxDistance,
        int fadeStartDistance,
        boolean enableOcclusion,
        boolean enableDebugLog
) implements VoiceConfig {
    @Override
    public VoiceMode mode() {
        return VoiceMode.REMOTE;
    }
}
