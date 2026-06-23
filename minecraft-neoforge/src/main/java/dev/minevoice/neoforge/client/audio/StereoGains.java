package dev.minevoice.neoforge.client.audio;

/**
 * Per-channel playback gains for one remote voice source.
 */
public record StereoGains(float left, float right) {
    public static final StereoGains CENTER = new StereoGains(0.70710677F, 0.70710677F);

    public StereoGains {
        left = clamp(left);
        right = clamp(right);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
