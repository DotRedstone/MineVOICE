package dev.minevoice.common.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public final class BinaryVoicePacketCodec implements VoicePacketCodec {
    private static final int MAGIC = 0x4D564F43;
    private static final int HEADER_BYTES = Integer.BYTES
            + Integer.BYTES
            + Integer.BYTES
            + Long.BYTES
            + Long.BYTES
            + Long.BYTES
            + Long.BYTES
            + Integer.BYTES;
    private static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    @Override
    public byte[] encode(VoicePacket packet) {
        byte[] payload = packet.payload() == null ? new byte[0] : packet.payload();
        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("voice packet payload is too large: " + payload.length);
        }

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + payload.length);
        buffer.putInt(MAGIC);
        buffer.putInt(packet.protocolVersion());
        buffer.putInt(packet.packetType().ordinal());
        UUID playerId = packet.playerId();
        buffer.putLong(playerId == null ? 0L : playerId.getMostSignificantBits());
        buffer.putLong(playerId == null ? 0L : playerId.getLeastSignificantBits());
        buffer.putLong(packet.sequence());
        buffer.putLong(packet.timestampMillis());
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    @Override
    public VoicePacket decode(byte[] bytes) {
        if (bytes.length < HEADER_BYTES) {
            throw new IllegalArgumentException("voice packet is shorter than header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("invalid voice packet magic");
        }

        int protocolVersion = buffer.getInt();
        int packetTypeOrdinal = buffer.getInt();
        VoicePacketType[] types = VoicePacketType.values();
        if (packetTypeOrdinal < 0 || packetTypeOrdinal >= types.length) {
            throw new IllegalArgumentException("unknown voice packet type: " + packetTypeOrdinal);
        }

        long uuidMost = buffer.getLong();
        long uuidLeast = buffer.getLong();
        UUID playerId = uuidMost == 0L && uuidLeast == 0L ? null : new UUID(uuidMost, uuidLeast);
        long sequence = buffer.getLong();
        long timestampMillis = buffer.getLong();
        int payloadLength = buffer.getInt();
        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES || payloadLength > buffer.remaining()) {
            throw new IllegalArgumentException("invalid voice packet payload length: " + payloadLength);
        }

        byte[] payload = Arrays.copyOfRange(bytes, HEADER_BYTES, HEADER_BYTES + payloadLength);
        return new VoicePacket(types[packetTypeOrdinal], protocolVersion, playerId, sequence, timestampMillis, payload);
    }
}
