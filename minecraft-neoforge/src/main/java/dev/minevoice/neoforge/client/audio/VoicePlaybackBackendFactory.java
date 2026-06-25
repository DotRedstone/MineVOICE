package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

import javax.sound.sampled.AudioFormat;

public final class VoicePlaybackBackendFactory {
    private VoicePlaybackBackendFactory() {
    }

    public static VoicePlaybackBackend open(ClientAudioSettings settings, AudioFormat format, int bufferBytes) {
        return JavaSoundVoicePlaybackBackend.open(settings, format, bufferBytes);
    }

    public static String supportedBackends() {
        return OpenAlVoicePlaybackBackend.available()
                ? JavaSoundVoicePlaybackBackend.NAME + "," + OpenAlVoicePlaybackBackend.NAME + "-planned"
                : JavaSoundVoicePlaybackBackend.NAME;
    }
}
