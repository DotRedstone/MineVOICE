package dev.minevoice.common.audio;

/**
 * Playback-side jitter buffer limits for one speaker.
 */
public record JitterBufferConfig(int targetDelayFrames, int maxBufferedFrames) {
    public static JitterBufferConfig defaultVoice() {
        return new JitterBufferConfig(3, 12);
    }

    public JitterBufferConfig {
        if (targetDelayFrames < 1) {
            throw new IllegalArgumentException("targetDelayFrames must be positive");
        }
        if (maxBufferedFrames < targetDelayFrames) {
            throw new IllegalArgumentException("maxBufferedFrames must be >= targetDelayFrames");
        }
    }
}
