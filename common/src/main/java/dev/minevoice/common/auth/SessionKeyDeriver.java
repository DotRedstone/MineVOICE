package dev.minevoice.common.auth;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class SessionKeyDeriver {
    private SessionKeyDeriver() {}

    public static byte[] derive(String sharedSecret, AuthToken token) {
        // Derive a 16-byte (128-bit) symmetric key for AES from the shared secret and token signature
        byte[] hash = HmacMessageSigner.sign(token.signature().getBytes(StandardCharsets.UTF_8), sharedSecret);
        return Arrays.copyOf(hash, 16);
    }
}
