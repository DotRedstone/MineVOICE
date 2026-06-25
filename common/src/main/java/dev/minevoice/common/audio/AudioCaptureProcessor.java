package dev.minevoice.common.audio;

/**
 * Extension point for microphone-side DSP such as noise suppression or echo cancellation.
 */
public interface AudioCaptureProcessor {
    /**
     * Processes one captured PCM frame on the audio capture thread.
     *
     * <p>Implementations must be fast and may return either the original PCM array or a replacement array.</p>
     */
    byte[] process(AudioCaptureProcessingRequest request);
}
