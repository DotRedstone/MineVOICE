package dev.minevoice.common.network;

import java.util.concurrent.atomic.AtomicLong;

public final class BandwidthCounter {
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();

    public void recordReceived(int bytes) {
        receivedBytes.addAndGet(bytes);
    }

    public void recordSent(int bytes) {
        sentBytes.addAndGet(bytes);
    }

    public long receivedBytes() {
        return receivedBytes.get();
    }

    public long sentBytes() {
        return sentBytes.get();
    }
}
