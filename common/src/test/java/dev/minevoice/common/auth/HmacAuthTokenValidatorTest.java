package dev.minevoice.common.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class HmacAuthTokenValidatorTest {
    @Test
    void acceptsTokenSignedWithSharedSecret() {
        HmacAuthTokenIssuer issuer = new HmacAuthTokenIssuer("secret");
        HmacAuthTokenValidator validator = new HmacAuthTokenValidator("secret");

        AuthToken token = issuer.issue(UUID.randomUUID(), "server", Duration.ofMinutes(1));

        assertTrue(validator.validate(token).valid());
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        HmacAuthTokenIssuer issuer = new HmacAuthTokenIssuer("secret");
        HmacAuthTokenValidator validator = new HmacAuthTokenValidator("other-secret");

        AuthToken token = issuer.issue(UUID.randomUUID(), "server", Duration.ofMinutes(1));

        assertFalse(validator.validate(token).valid());
    }
}
