package dev.minevoice.common.audio;

import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.common.protocol.VoiceFrame;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class JitterBufferTest {
    private final UUID speaker = UUID.randomUUID();

    @Test
    void reordersFramesBySequenceAfterTargetDelay() {
        JitterBuffer buffer = new JitterBuffer(new JitterBufferConfig(2, 4));
        buffer.offer(frame(2, 2));
        assertNull(buffer.pollReady());

        buffer.offer(frame(1, 1));
        assertEquals(1, buffer.pollReady().sequence());
        assertEquals(2, buffer.pollReady().sequence());
    }

    @Test
    void dropsLateFramesAfterPlaybackMovedPastSequence() {
        JitterBuffer buffer = new JitterBuffer(new JitterBufferConfig(1, 4));
        buffer.offer(frame(1, 1));
        assertEquals(1, buffer.pollReady().sequence());

        buffer.offer(frame(1, 9));
        assertNull(buffer.pollReady());
        assertEquals(1, buffer.stats().latePackets());
    }

    @Test
    void skipsMissingSequenceWithoutCorruptingPayload() {
        JitterBuffer buffer = new JitterBuffer(new JitterBufferConfig(2, 4));
        buffer.offer(frame(1, 1));
        buffer.offer(frame(3, 3));

        assertEquals(1, buffer.pollReady().sequence());
        VoiceFrame recovered = buffer.pollReady();
        assertEquals(3, recovered.sequence());
        assertArrayEquals(new byte[]{3}, recovered.encodedAudio());
        assertEquals(1, buffer.stats().missingFrames());
    }

    private VoiceFrame frame(long sequence, int payload) {
        return new VoiceFrame(
                speaker,
                sequence,
                System.currentTimeMillis(),
                new byte[]{(byte) payload},
                48_000,
                1,
                VoiceChannel.PROXIMITY
        );
    }
}
