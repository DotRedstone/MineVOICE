package dev.minevoice.common.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class HmacAuthTokenIssuer implements AuthTokenIssuer {
    private final String sharedSecret;

    public HmacAuthTokenIssuer(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @Override
    public AuthToken issue(UUID playerUuid, String serverId, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String signature = AuthTokenSigner.sign(playerUuid, now, expiresAt, serverId, sharedSecret);
        return new AuthToken(playerUuid, now, expiresAt, serverId, signature);
    }
}
