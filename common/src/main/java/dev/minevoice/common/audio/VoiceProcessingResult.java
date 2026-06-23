package dev.minevoice.common.audio;

/**
 * Per-listener playback parameters returned by a voice-processing algorithm.
 */
public record VoiceProcessingResult(byte[] encodedAudio, float gain, float pan) {
    public VoiceProcessingResult {
        gain = Math.max(0.0F, gain);
        pan = Math.max(-1.0F, Math.min(1.0F, pan));
    }
}
