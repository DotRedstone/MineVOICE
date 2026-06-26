package dev.minevoice.common.session;

import java.time.Instant;
import java.util.UUID;

public record VoiceSession(
        UUID sessionId,
        VoicePlayerInfo playerInfo,
        VoiceSessionState state,
        Instant createdAt,
        Instant lastActivityAt,
        byte[] sessionKey
) {
    public VoiceSession(UUID sessionId, VoicePlayerInfo playerInfo, VoiceSessionState state, Instant createdAt, Instant lastActivityAt) {
        this(sessionId, playerInfo, state, createdAt, lastActivityAt, null);
    }
}
