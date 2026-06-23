package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.protocol.VoiceChannel;

import java.util.UUID;

/**
 * Resolves listener-relative stereo gains without owning audio capture or playback.
 */
@FunctionalInterface
public interface VoiceSpatializer {
    StereoGains gainsFor(UUID speakerId, VoiceChannel channel);
}
