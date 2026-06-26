package dev.minevoice.common.audio;

/**
 * Smooths voice activity detection so a signal near the threshold does not flicker per frame.
 */
public final class VoiceActivityGate {
    private static final float DEFAULT_NOISE_FLOOR = 0.001F;
    private static final float DEFAULT_CLOSE_MARGIN = 0.01F;
    private static final int DEFAULT_HANGOVER_FRAMES = 25;

    private final float noiseFloor;
    private final float closeMargin;
    private final int hangoverFrames;
    private boolean open;
    private int hangover;

    public VoiceActivityGate(float noiseFloor, float closeMargin, int hangoverFrames) {
        this.noiseFloor = clamp(noiseFloor);
        this.closeMargin = Math.max(0.0F, closeMargin);
        this.hangoverFrames = Math.max(0, hangoverFrames);
    }

    public static VoiceActivityGate defaultVoice() {
        return new VoiceActivityGate(DEFAULT_NOISE_FLOOR, DEFAULT_CLOSE_MARGIN, DEFAULT_HANGOVER_FRAMES);
    }

    public boolean update(float level, float configuredThreshold) {
        float openThreshold = Math.max(noiseFloor, clamp(configuredThreshold));
        float closeThreshold = Math.max(noiseFloor, openThreshold - closeMargin);
        float clampedLevel = clamp(level);
        if (clampedLevel >= openThreshold) {
            open = true;
            hangover = hangoverFrames;
            return true;
        }
        if (open && clampedLevel >= closeThreshold) {
            hangover = hangoverFrames;
            return true;
        }
        if (open && hangover > 0) {
            hangover--;
            return true;
        }
        open = false;
        return false;
    }

    public void reset() {
        open = false;
        hangover = 0;
    }

    public boolean open() {
        return open;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
