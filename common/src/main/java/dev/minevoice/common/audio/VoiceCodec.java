package dev.minevoice.common.audio;

/**
 * Encodes and decodes voice frames. Production code should provide an Opus implementation.
 */
public interface VoiceCodec {
    byte[] encode(byte[] pcmSamples);

    byte[] decode(byte[] encodedAudio);
}
