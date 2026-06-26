package dev.minevoice.neoforge.client.audio;

import java.util.UUID;

public record VoiceSourceSnapshot(
        UUID speakerId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean known,
        boolean occluded,
        float directGain,
        float highFrequencyGain,
        float reflectionGain,
        int reflectionProbeCount
) {
    public static VoiceSourceSnapshot unknown(UUID speakerId) {
        return new VoiceSourceSnapshot(speakerId, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, false, false, 1.0F, 1.0F, 0.0F, 0);
    }
}
