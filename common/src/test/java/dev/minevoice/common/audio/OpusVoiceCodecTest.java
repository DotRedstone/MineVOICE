package dev.minevoice.common.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpusVoiceCodecTest {
    @Test
    void encodesAndDecodesOneVoiceFrame() {
        VoiceAudioFormat format = VoiceAudioFormat.narrowbandVoice();
        OpusVoiceCodec codec = new OpusVoiceCodec(format);
        byte[] pcm = sineFrame(format);

        byte[] encoded = codec.encode(pcm);
        byte[] decoded = codec.createDecoder().decode(encoded);

        assertTrue(encoded.length > 0);
        assertTrue(encoded.length < pcm.length);
        assertEquals(pcm.length, decoded.length);
    }

    @Test
    void factoryCreatesOpusForConfiguredOpus() {
        VoiceCodec codec = VoiceCodecFactory.create("opus");

        assertEquals("opus", codec.codecName());
    }

    private static byte[] sineFrame(VoiceAudioFormat format) {
        int sampleCount = format.sampleRate() * format.frameDurationMillis() / 1_000 * format.channels();
        byte[] pcm = new byte[sampleCount * Short.BYTES];
        for (int index = 0; index < sampleCount; index++) {
            double angle = 2.0D * Math.PI * 440.0D * index / format.sampleRate();
            short sample = (short) Math.round(Math.sin(angle) * Short.MAX_VALUE * 0.35D);
            int offset = index * Short.BYTES;
            pcm[offset] = (byte) (sample & 0xFF);
            pcm[offset + 1] = (byte) ((sample >>> 8) & 0xFF);
        }
        return pcm;
    }
}
