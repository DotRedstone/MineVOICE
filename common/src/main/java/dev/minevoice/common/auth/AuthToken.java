package dev.minevoice.common.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthToken(
        UUID playerUuid,
        Instant issuedAt,
        Instant expiresAt,
        String serverId,
        String signature
) {
}
