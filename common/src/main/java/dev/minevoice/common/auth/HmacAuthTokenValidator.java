package dev.minevoice.common.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class HmacAuthTokenValidator implements AuthTokenValidator {
    private final String sharedSecret;
    private final Clock clock;
    private final Duration clockSkewTolerance;

    public HmacAuthTokenValidator(String sharedSecret) {
        this(sharedSecret, Clock.systemUTC(), Duration.ofSeconds(10));
    }

    public HmacAuthTokenValidator(String sharedSecret, Clock clock, Duration clockSkewTolerance) {
        this.sharedSecret = sharedSecret;
        this.clock = clock;
        this.clockSkewTolerance = clockSkewTolerance;
    }

    @Override
    public TokenValidationResult validate(AuthToken token) {
        Instant now = Instant.now(clock);
        if (token.issuedAt().isAfter(now.plus(clockSkewTolerance))) {
            return TokenValidationResult.rejected("token issued in the future");
        }
        if (token.expiresAt().isBefore(now.minus(clockSkewTolerance))) {
            return TokenValidationResult.rejected("token expired");
        }
        if (!AuthTokenSigner.matches(token, sharedSecret)) {
            return TokenValidationResult.rejected("token signature mismatch");
        }
        return TokenValidationResult.accepted();
    }
}
