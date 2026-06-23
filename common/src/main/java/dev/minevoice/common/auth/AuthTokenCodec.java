package dev.minevoice.common.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class AuthTokenCodec {
    private AuthTokenCodec() {
    }

    public static byte[] encodeToBytes(AuthToken token) {
        return encodeToString(token).getBytes(StandardCharsets.UTF_8);
    }

    public static String encodeToString(AuthToken token) {
        String raw = token.playerUuid()
                + "|" + token.issuedAt().toEpochMilli()
                + "|" + token.expiresAt().toEpochMilli()
                + "|" + token.serverId()
                + "|" + token.signature();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static AuthToken decodeFromBytes(byte[] bytes) {
        return decodeFromString(new String(bytes, StandardCharsets.UTF_8));
    }

    public static AuthToken decodeFromString(String encoded) {
        String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("invalid auth token field count");
        }
        return new AuthToken(
                UUID.fromString(parts[0]),
                Instant.ofEpochMilli(Long.parseLong(parts[1])),
                Instant.ofEpochMilli(Long.parseLong(parts[2])),
                parts[3],
                parts[4]
        );
    }
}
