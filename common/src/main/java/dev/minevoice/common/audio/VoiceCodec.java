package dev.minevoice.common.audio;

/**
 * Encodes and decodes voice frames. Production code should provide an Opus implementation.
 */
public interface VoiceCodec {
    default String codecName() {
        return getClass().getSimpleName();
    }

    byte[] encode(byte[] pcmSamples);

    byte[] decode(byte[] encodedAudio);
}
