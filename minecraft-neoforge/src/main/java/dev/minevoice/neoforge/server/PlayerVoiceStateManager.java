package dev.minevoice.neoforge.server;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerVoiceStateManager {
    private final Map<UUID, Boolean> connectedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> mutedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> mutedPeers = new ConcurrentHashMap<>();

    public void markConnected(UUID playerId) {
        connectedPlayers.put(playerId, true);
        mutedPlayers.putIfAbsent(playerId, false);
    }

    public void markDisconnected(UUID playerId) {
        connectedPlayers.remove(playerId);
        mutedPlayers.remove(playerId);
        mutedPeers.remove(playerId);
        mutedPeers.values().forEach(peers -> peers.remove(playerId));
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

    public void setPeerMuted(UUID playerId, UUID peerId, boolean muted) {
        if (!isConnected(playerId) || playerId.equals(peerId)) {
            return;
        }
        Set<UUID> peers = mutedPeers.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
        if (muted) {
            peers.add(peerId);
        } else {
            peers.remove(peerId);
        }
    }

    public Set<UUID> mutedPeers(UUID playerId) {
        Set<UUID> peers = mutedPeers.get(playerId);
        return peers == null ? Set.of() : Set.copyOf(peers);
    }
}
