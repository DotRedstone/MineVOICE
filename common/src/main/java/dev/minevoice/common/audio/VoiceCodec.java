package dev.minevoice.common.audio;

/**
 * Encodes outgoing voice frames and creates per-speaker decoders for incoming streams.
 */
public interface VoiceCodec {
    default String codecName() {
        return getClass().getSimpleName();
    }

    byte[] encode(byte[] pcmSamples);

    byte[] decode(byte[] encodedAudio);

    default VoiceDecoder createDecoder() {
        return this::decode;
    }
}
