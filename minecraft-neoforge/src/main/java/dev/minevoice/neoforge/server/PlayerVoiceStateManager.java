package dev.minevoice.neoforge.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerVoiceStateManager {
    private final Map<UUID, Boolean> connectedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> mutedPlayers = new ConcurrentHashMap<>();

    public void markConnected(UUID playerId) {
        connectedPlayers.put(playerId, true);
        mutedPlayers.putIfAbsent(playerId, false);
    }

    public void markDisconnected(UUID playerId) {
        connectedPlayers.remove(playerId);
        mutedPlayers.remove(playerId);
    }

    public boolean isConnected(UUID playerId) {
        return connectedPlayers.containsKey(playerId);
    }

    public void setMuted(UUID playerId, boolean muted) {
        if (isConnected(playerId)) {
            mutedPlayers.put(playerId, muted);
        }
    }

    public boolean muted(UUID playerId) {
        return mutedPlayers.getOrDefault(playerId, false);
    }
}
