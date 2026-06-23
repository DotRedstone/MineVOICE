package dev.minevoice.common.auth;

import java.time.Duration;
import java.util.UUID;

/**
 * Issues temporary voice tokens from the trusted Minecraft server side.
 */
public interface AuthTokenIssuer {
    AuthToken issue(UUID playerUuid, String serverId, Duration ttl);
}
