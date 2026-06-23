package dev.minevoice.standalone.metrics;

import dev.minevoice.common.network.BandwidthCounter;

public record BandwidthStats(long receivedBytes, long sentBytes) {
    public static BandwidthStats from(BandwidthCounter counter) {
        return new BandwidthStats(counter.receivedBytes(), counter.sentBytes());
    }
}
