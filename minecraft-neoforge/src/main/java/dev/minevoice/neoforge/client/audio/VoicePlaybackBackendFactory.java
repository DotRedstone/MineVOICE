package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

import javax.sound.sampled.AudioFormat;

public final class VoicePlaybackBackendFactory {
    private VoicePlaybackBackendFactory() {
    }

    public static VoicePlaybackBackend open(ClientAudioSettings settings, AudioFormat format, int bufferBytes) {
        String requestedBackend = normalizeBackendName(settings.audioPlaybackBackend());
        if (OpenAlVoicePlaybackBackend.NAME.equals(requestedBackend) || "auto".equals(requestedBackend)) {
            if (OpenAlVoicePlaybackBackend.available()) {
                try {
                    return OpenAlVoicePlaybackBackend.open(settings);
                } catch (RuntimeException exception) {
                    // Fallback to JavaSound if OpenAL fails
                }
            }
        }
        return JavaSoundVoicePlaybackBackend.open(settings, format, bufferBytes);
    }

    public static String normalizeBackendName(String value) {
        if (value == null || value.isBlank()) {
            return "auto";
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        if ("java".equals(normalized) || "javasound".equals(normalized)) {
            return JavaSoundVoicePlaybackBackend.NAME;
        }
        if (OpenAlVoicePlaybackBackend.NAME.equals(normalized)) {
            return OpenAlVoicePlaybackBackend.NAME;
        }
        return "auto";
    }

    public static String supportedBackends() {
        return OpenAlVoicePlaybackBackend.available()
                ? JavaSoundVoicePlaybackBackend.NAME + "," + OpenAlVoicePlaybackBackend.NAME
                : JavaSoundVoicePlaybackBackend.NAME;
    }
}
