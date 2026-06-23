package dev.minevoice.common.protocol;

/**
 * Converts wire packets to bytes without owning transport, auth, or audio logic.
 */
public interface VoicePacketCodec {
    byte[] encode(VoicePacket packet);

    VoicePacket decode(byte[] bytes);
}
