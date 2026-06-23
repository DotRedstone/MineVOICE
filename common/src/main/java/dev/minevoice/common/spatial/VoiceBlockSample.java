package dev.minevoice.common.spatial;

import java.util.Objects;

/**
 * A compact, server-authoritative description of a block supplied to a voice algorithm.
 */
public record VoiceBlockSample(
        int x,
        int y,
        int z,
        String blockStateId,
        boolean occluding,
        float absorption
) {
    public VoiceBlockSample {
        blockStateId = Objects.requireNonNullElse(blockStateId, "minecraft:air");
        absorption = Math.max(0.0F, Math.min(1.0F, absorption));
    }
}
