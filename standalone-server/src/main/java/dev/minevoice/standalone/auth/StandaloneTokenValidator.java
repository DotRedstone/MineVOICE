package dev.minevoice.standalone.auth;

import dev.minevoice.common.auth.AuthToken;
import dev.minevoice.common.auth.AuthTokenValidator;
import dev.minevoice.common.auth.HmacAuthTokenValidator;
import dev.minevoice.common.auth.TokenValidationResult;

public final class StandaloneTokenValidator implements AuthTokenValidator {
    private final HmacAuthTokenValidator validator;

    public StandaloneTokenValidator(String sharedSecret) {
        this.validator = new HmacAuthTokenValidator(sharedSecret);
    }

    @Override
    public TokenValidationResult validate(AuthToken token) {
        return validator.validate(token);
    }
}
