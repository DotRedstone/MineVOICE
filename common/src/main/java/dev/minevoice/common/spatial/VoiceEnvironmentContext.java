package dev.minevoice.common.spatial;

import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoicePlayerState;

import java.util.List;

/**
 * Immutable world context paired with one routed voice frame for audio processing.
 */
public record VoiceEnvironmentContext(
        VoicePlayerState sender,
        VoicePlayerState listener,
        VoiceChannel channel,
        List<VoiceBlockSample> blockSamples
) {
    public VoiceEnvironmentContext {
        blockSamples = List.copyOf(blockSamples);
    }
}
