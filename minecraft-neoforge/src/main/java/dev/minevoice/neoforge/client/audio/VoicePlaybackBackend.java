package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.neoforge.client.ClientAudioSettings;

import java.util.Set;
import java.util.UUID;

public interface VoicePlaybackBackend extends AutoCloseable {
    String backendName();

    void start();

    void writeStereoFrame(byte[] pcm, int offset, int length);

    boolean matches(ClientAudioSettings settings);

    default boolean supportsSourcePlayback() {
        return false;
    }

    default void updateListener(VoiceListenerSnapshot listener) {
    }

    default void writeSourceFrame(
            UUID speakerId,
            VoiceChannel channel,
            VoiceSourceSnapshot source,
            byte[] pcm,
            int sampleRate,
            float gain
    ) {
        throw new UnsupportedOperationException("source playback is not supported by " + backendName());
    }

    default void retainSources(Set<UUID> activeSpeakers) {
    }

    @Override
    void close();
}
