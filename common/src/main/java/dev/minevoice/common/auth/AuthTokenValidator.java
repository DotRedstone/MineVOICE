package dev.minevoice.common.auth;

/**
 * Validates a token before a voice client can join a session.
 */
public interface AuthTokenValidator {
    TokenValidationResult validate(AuthToken token);
}
