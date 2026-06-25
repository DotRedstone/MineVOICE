package dev.minevoice.neoforge.client.audio;

import dev.minevoice.neoforge.client.ClientAudioSettings;

public interface VoicePlaybackBackend extends AutoCloseable {
    String backendName();

    void start();

    void writeStereoFrame(byte[] pcm, int offset, int length);

    boolean matches(ClientAudioSettings settings);

    @Override
    void close();
}
