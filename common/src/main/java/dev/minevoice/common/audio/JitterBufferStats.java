package dev.minevoice.common.audio;

public record JitterBufferStats(
        int bufferedFrames,
        long latePackets,
        long droppedPackets,
        long missingFrames
) {
}
