package dev.minevoice.common.spatial;

public final class BasicSpatialAudioProcessor implements SpatialAudioProcessor {
    private final DistanceCulling distanceCulling;

    public BasicSpatialAudioProcessor(DistanceCulling distanceCulling) {
        this.distanceCulling = distanceCulling;
    }

    @Override
    public SpatialAudioResult process(double distance, double horizontalOffset) {
        if (!distanceCulling.isAudible(distance)) {
            return new SpatialAudioResult(0.0F, 0.0F, false);
        }
        float volume = distanceCulling.volumeAt(distance);
        float pan = (float) Math.max(-1.0D, Math.min(1.0D, horizontalOffset));
        return new SpatialAudioResult(volume, pan, true);
    }
}
