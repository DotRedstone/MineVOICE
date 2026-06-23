package dev.minevoice.common.protocol;

import java.util.UUID;

public record VoiceFrame(
        UUID senderPlayerId,
        long sequence,
        long timestampMillis,
        byte[] encodedAudio,
        int sampleRate,
        int channels,
        VoiceChannel channel
) {
    public VoiceFrame(
            UUID senderPlayerId,
            long sequence,
            long timestampMillis,
            byte[] encodedAudio,
            int sampleRate,
            int channels
    ) {
        this(senderPlayerId, sequence, timestampMillis, encodedAudio, sampleRate, channels, VoiceChannel.PROXIMITY);
    }
}
