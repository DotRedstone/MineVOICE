package dev.minevoice.common.config;

/**
 * Shared voice configuration visible to Minecraft and voice-server modules.
 */
public interface VoiceConfig {
    VoiceMode mode();

    int maxDistance();

    int fadeStartDistance();

    boolean enableOcclusion();

    boolean enableDebugLog();
}
