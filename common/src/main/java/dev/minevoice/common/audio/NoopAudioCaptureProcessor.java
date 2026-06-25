package dev.minevoice.common.audio;

public enum NoopAudioCaptureProcessor implements AudioCaptureProcessor {
    INSTANCE;

    @Override
    public byte[] process(AudioCaptureProcessingRequest request) {
        return request.pcmSamples();
    }
}
