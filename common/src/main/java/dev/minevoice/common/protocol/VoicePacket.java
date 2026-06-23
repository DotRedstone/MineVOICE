package dev.minevoice.common.protocol;

import java.util.UUID;

public record VoicePacket(
        VoicePacketType packetType,
        int protocolVersion,
        UUID playerId,
        long sequence,
        long timestampMillis,
        byte[] payload
) {
}
