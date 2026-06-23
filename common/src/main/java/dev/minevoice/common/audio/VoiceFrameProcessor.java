package dev.minevoice.common.audio;

/**
 * Extension point for occlusion, 3D positioning, noise treatment, or other per-listener audio work.
 */
public interface VoiceFrameProcessor {
    /**
     * Processes one routed frame without mutating the supplied request.
     */
    VoiceProcessingResult process(VoiceProcessingRequest request);
}
