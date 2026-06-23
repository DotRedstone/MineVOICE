package dev.minevoice.common.audio;

import dev.minevoice.common.protocol.VoiceFrame;
import dev.minevoice.common.spatial.VoiceEnvironmentContext;

/**
 * Audio frame and world context supplied to a pluggable voice-processing algorithm.
 */
public record VoiceProcessingRequest(VoiceFrame frame, VoiceEnvironmentContext environment) {
}
