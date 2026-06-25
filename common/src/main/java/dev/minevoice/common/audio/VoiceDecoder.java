package dev.minevoice.common.audio;

/**
 * Stateful decoder for one remote speaker stream.
 */
@FunctionalInterface
public interface VoiceDecoder {
    byte[] decode(byte[] encodedAudio);
}
