package dev.minevoice.common.audio;

import dev.minevoice.common.protocol.VoiceFrame;

import java.util.Map;
import java.util.TreeMap;

public final class JitterBuffer {
    private final JitterBufferConfig config;
    private final TreeMap<Long, VoiceFrame> frames = new TreeMap<>();
    private boolean ready;
    private long nextSequence = -1L;
    private long latePackets;
    private long droppedPackets;
    private long missingFrames;

    public JitterBuffer(JitterBufferConfig config) {
        this.config = config;
    }

    public void offer(VoiceFrame frame) {
        if (nextSequence >= 0L && frame.sequence() < nextSequence) {
            if (!ready) {
                nextSequence = frame.sequence();
            } else {
                latePackets++;
                return;
            }
        }
        frames.put(frame.sequence(), frame);
        if (nextSequence < 0L || (!ready && frame.sequence() < nextSequence)) {
            nextSequence = frame.sequence();
        }
        trimOverflow();
    }

    public VoiceFrame pollReady() {
        if (frames.isEmpty()) {
            ready = false;
            return null;
        }
        if (!ready && frames.size() < config.targetDelayFrames()) {
            return null;
        }
        ready = true;

        VoiceFrame exact = frames.remove(nextSequence);
        if (exact != null) {
            nextSequence++;
            return exact;
        }

        Map.Entry<Long, VoiceFrame> first = frames.firstEntry();
        if (first == null) {
            return null;
        }
        if (first.getKey() > nextSequence) {
            missingFrames += first.getKey() - nextSequence;
            nextSequence = first.getKey();
        }
        VoiceFrame recovered = frames.pollFirstEntry().getValue();
        nextSequence = recovered.sequence() + 1L;
        return recovered;
    }

    public boolean hasBufferedFrames() {
        return !frames.isEmpty();
    }

    public JitterBufferStats stats() {
        return new JitterBufferStats(frames.size(), latePackets, droppedPackets, missingFrames);
    }

    private void trimOverflow() {
        while (frames.size() > config.maxBufferedFrames()) {
            Map.Entry<Long, VoiceFrame> first = frames.pollFirstEntry();
            if (first == null) {
                return;
            }
            if (first.getKey() >= nextSequence) {
                nextSequence = first.getKey() + 1L;
            }
            droppedPackets++;
        }
    }
}
