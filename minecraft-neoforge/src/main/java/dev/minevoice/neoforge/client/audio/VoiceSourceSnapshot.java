package dev.minevoice.neoforge.client.audio;

import java.util.UUID;

public record VoiceSourceSnapshot(
        UUID speakerId,
        double x,
        double y,
        double z,
        boolean known,
        boolean occluded
) {
    public static VoiceSourceSnapshot unknown(UUID speakerId) {
        return new VoiceSourceSnapshot(speakerId, 0.0D, 0.0D, 0.0D, false, false);
    }
}
