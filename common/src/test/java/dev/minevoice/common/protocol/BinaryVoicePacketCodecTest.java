package dev.minevoice.common.protocol;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class BinaryVoicePacketCodecTest {
    @Test
    void roundTripsPacketFields() {
        BinaryVoicePacketCodec codec = new BinaryVoicePacketCodec();
        UUID playerId = UUID.randomUUID();
        VoicePacket packet = new VoicePacket(
                VoicePacketType.PING,
                VoiceProtocolVersion.CURRENT,
                playerId,
                42L,
                1234L,
                new byte[]{1, 2, 3}
        );

        VoicePacket decoded = codec.decode(codec.encode(packet));

        assertEquals(packet.packetType(), decoded.packetType());
        assertEquals(packet.protocolVersion(), decoded.protocolVersion());
        assertEquals(packet.playerId(), decoded.playerId());
        assertEquals(packet.sequence(), decoded.sequence());
        assertEquals(packet.timestampMillis(), decoded.timestampMillis());
        assertArrayEquals(packet.payload(), decoded.payload());
    }

    @Test
    void rejectsInvalidPacket() {
        BinaryVoicePacketCodec codec = new BinaryVoicePacketCodec();

        assertThrows(IllegalArgumentException.class, () -> codec.decode(new byte[]{1, 2, 3}));
    }
}
