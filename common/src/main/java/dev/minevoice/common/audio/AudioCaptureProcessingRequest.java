package dev.minevoice.common.audio;

/**
 * One captured PCM frame before codec encoding.
 */
public record AudioCaptureProcessingRequest(
        byte[] pcmSamples,
        VoiceAudioFormat format,
        float microphoneLevel,
        long timestampMillis
) {
}
