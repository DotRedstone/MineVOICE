package dev.minevoice.common.spatial;

/**
 * Computes playback hints for a listener without owning audio output.
 */
public interface SpatialAudioProcessor {
    SpatialAudioResult process(double distance, double horizontalOffset);
}
