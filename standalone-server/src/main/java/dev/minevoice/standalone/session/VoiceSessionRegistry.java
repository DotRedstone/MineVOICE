package dev.minevoice.standalone.session;

import dev.minevoice.common.session.VoiceSession;
import dev.minevoice.common.session.VoiceSessionState;
import dev.minevoice.common.protocol.VoicePlayerState;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceSessionRegistry {
    private final Map<UUID, VoiceSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, VoicePlayerState> playerStates = new ConcurrentHashMap<>();

    public void register(VoiceSession session) {
        sessions.put(session.sessionId(), session);
    }

    public void disconnect(UUID sessionId) {
        sessions.remove(sessionId);
    }

    public VoiceSession find(UUID sessionId) {
        return sessions.get(sessionId);
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
        sessions.computeIfPresent(sessionId, (ignored, session) -> new VoiceSession(
                session.sessionId(),
                session.playerInfo(),
                session.state(),
                session.createdAt(),
                Instant.now()
        ));
    }

    public void removeExpired(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        sessions.entrySet().removeIf(entry -> entry.getValue().lastActivityAt().isBefore(cutoff)
                || entry.getValue().state() == VoiceSessionState.DISCONNECTED);
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
    }

    public VoicePlayerState playerState(UUID playerId) {
        return playerStates.get(playerId);
    }

    public int size() {
        return sessions.size();
    }
}
