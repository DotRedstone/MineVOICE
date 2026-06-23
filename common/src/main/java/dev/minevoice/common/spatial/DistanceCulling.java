package dev.minevoice.common.spatial;

public final class DistanceCulling {
    private final double fadeStartDistance;
    private final double maxDistance;

    public DistanceCulling(double fadeStartDistance, double maxDistance) {
        if (fadeStartDistance < 0 || maxDistance <= 0 || fadeStartDistance > maxDistance) {
            throw new IllegalArgumentException("invalid distance culling range");
        }
        this.fadeStartDistance = fadeStartDistance;
        this.maxDistance = maxDistance;
    }

    public boolean isAudible(double distance) {
        return distance <= maxDistance;
    }

    public float volumeAt(double distance) {
        if (distance <= fadeStartDistance) {
            return 1.0F;
        }
        if (distance >= maxDistance) {
            return 0.0F;
        }
        double fadeRange = maxDistance - fadeStartDistance;
        return (float) (1.0D - ((distance - fadeStartDistance) / fadeRange));
    }
}
