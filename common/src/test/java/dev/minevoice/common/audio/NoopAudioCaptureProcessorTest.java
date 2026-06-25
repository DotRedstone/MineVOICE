package dev.minevoice.common.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

final class NoopAudioCaptureProcessorTest {
    @Test
    void returnsOriginalPcmFrame() {
        byte[] pcm = new byte[]{1, 2, 3, 4};
        AudioCaptureProcessingRequest request = new AudioCaptureProcessingRequest(
                pcm,
                VoiceAudioFormat.narrowbandVoice(),
                0.5F,
                123L
        );

        assertSame(pcm, NoopAudioCaptureProcessor.INSTANCE.process(request));
    }
}
