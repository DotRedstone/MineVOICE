package dev.minevoice.server.session;

import dev.minevoice.common.session.VoiceSession;
import dev.minevoice.common.session.VoiceSessionState;
import dev.minevoice.common.protocol.VoicePlayerState;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class VoiceSessionRegistry {
    private final Map<UUID, VoiceSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, VoiceSession> sessionByPlayerId = new ConcurrentHashMap<>();
    private final Map<UUID, VoicePlayerState> playerStates = new ConcurrentHashMap<>();
    
    // Spatial Hash Grid for O(K) proximity routing
    private final Map<String, List<VoicePlayerState>> spatialGrid = new ConcurrentHashMap<>();
    private static final double GRID_CELL_SIZE = 48.0;

    public void register(VoiceSession session) {
        sessions.put(session.sessionId(), session);
        sessionByPlayerId.put(session.playerInfo().playerUuid(), session);
    }

    public void disconnect(UUID sessionId) {
        VoiceSession session = sessions.remove(sessionId);
        if (session != null) {
            sessionByPlayerId.remove(session.playerInfo().playerUuid());
        }
    }

    public VoiceSession find(UUID sessionId) {
        return sessions.get(sessionId);
    }

    public VoiceSession sessionByPlayerId(UUID playerId) {
        return sessionByPlayerId.get(playerId);
    }

    public boolean matchesEndpoint(UUID sessionId, String host, int port) {
        VoiceSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        return session.playerInfo().endpoint().host().equals(host)
                && session.playerInfo().endpoint().port() == port;
    }

    public void touch(UUID sessionId) {
        sessions.computeIfPresent(sessionId, (ignored, session) -> {
            VoiceSession updated = new VoiceSession(
                session.sessionId(),
                session.playerInfo(),
                session.state(),
                session.createdAt(),
                Instant.now(),
                session.sessionKey()
            );
            sessionByPlayerId.put(session.playerInfo().playerUuid(), updated);
            return updated;
        });
    }

    public void removeExpired(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        sessions.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().lastActivityAt().isBefore(cutoff)
                    || entry.getValue().state() == VoiceSessionState.DISCONNECTED;
            if (expired) {
                sessionByPlayerId.remove(entry.getValue().playerInfo().playerUuid());
            }
            return expired;
        });
    }

    public Collection<VoiceSession> activeSessions() {
        return List.copyOf(sessions.values());
    }

    public void replacePlayerStates(Collection<VoicePlayerState> states) {
        Map<UUID, VoicePlayerState> next = new ConcurrentHashMap<>();
        for (VoicePlayerState state : states) {
            next.put(state.playerId(), state);
        }
        playerStates.clear();
        playerStates.putAll(next);
        
        // Rebuild Spatial Grid
        Map<String, List<VoicePlayerState>> nextGrid = states.stream()
                .collect(Collectors.groupingBy(this::computeGridKey));
        spatialGrid.clear();
        spatialGrid.putAll(nextGrid);
    }

    public VoicePlayerState playerState(UUID playerId) {
        return playerStates.get(playerId);
    }
    
    public List<VoicePlayerState> getPlayersInGridCells(String dimensionId, double x, double z) {
        int cellX = (int) Math.floor(x / GRID_CELL_SIZE);
        int cellZ = (int) Math.floor(z / GRID_CELL_SIZE);
        
        java.util.ArrayList<VoicePlayerState> nearby = new java.util.ArrayList<>();
        // 3x3 grid around the player guarantees coverage up to GRID_CELL_SIZE radius
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String key = dimensionId + ":" + (cellX + dx) + ":" + (cellZ + dz);
                List<VoicePlayerState> cell = spatialGrid.get(key);
                if (cell != null) {
                    nearby.addAll(cell);
                }
            }
        }
        return nearby;
    }

    private String computeGridKey(VoicePlayerState state) {
        int cellX = (int) Math.floor(state.x() / GRID_CELL_SIZE);
        int cellZ = (int) Math.floor(state.z() / GRID_CELL_SIZE);
        return state.dimensionId() + ":" + cellX + ":" + cellZ;
    }

    public int size() {
        return sessions.size();
    }
}
