package dev.minevoice.neoforge.client;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoiceSpeakerTracker {
    private static final long SPEAKING_TIMEOUT_MILLIS = 900L;
    private final Map<UUID, Long> lastHeardAt = new LinkedHashMap<>();

    public synchronized void markSpeaking(UUID playerId) {
        lastHeardAt.put(playerId, System.currentTimeMillis());
    }

    public synchronized boolean isSpeaking(UUID playerId) {
        Long lastSeen = lastHeardAt.get(playerId);
        return lastSeen != null && System.currentTimeMillis() - lastSeen <= SPEAKING_TIMEOUT_MILLIS;
    }

    public synchronized List<UUID> activeSpeakers(int limit) {
        long cutoff = System.currentTimeMillis() - SPEAKING_TIMEOUT_MILLIS;
        lastHeardAt.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        return lastHeardAt.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(0, limit))
                .map(Map.Entry::getKey)
                .toList();
    }
}
