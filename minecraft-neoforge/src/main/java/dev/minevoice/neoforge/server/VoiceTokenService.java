package dev.minevoice.neoforge.server;

import dev.minevoice.common.auth.AuthToken;
import dev.minevoice.common.auth.AuthTokenIssuer;
import dev.minevoice.common.auth.HmacAuthTokenIssuer;

import java.time.Duration;
import java.util.UUID;

public final class VoiceTokenService implements AuthTokenIssuer {
    private final HmacAuthTokenIssuer issuer;

    public VoiceTokenService() {
        this("change-me");
    }

    public VoiceTokenService(String sharedSecret) {
        this.issuer = new HmacAuthTokenIssuer(sharedSecret);
    }

    @Override
    public AuthToken issue(UUID playerUuid, String serverId, Duration ttl) {
        return issuer.issue(playerUuid, serverId, ttl);
    }
}
