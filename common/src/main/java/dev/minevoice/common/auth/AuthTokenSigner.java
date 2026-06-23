package dev.minevoice.common.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class AuthTokenSigner {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private AuthTokenSigner() {
    }

    public static String sign(UUID playerUuid, Instant issuedAt, Instant expiresAt, String serverId, String sharedSecret) {
        String message = playerUuid + "|" + issuedAt.toEpochMilli() + "|" + expiresAt.toEpochMilli() + "|" + serverId;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign auth token", exception);
        }
    }

    public static boolean matches(AuthToken token, String sharedSecret) {
        String expected = sign(token.playerUuid(), token.issuedAt(), token.expiresAt(), token.serverId(), sharedSecret);
        return constantTimeEquals(expected, token.signature());
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        int diff = leftBytes.length ^ rightBytes.length;
        int count = Math.min(leftBytes.length, rightBytes.length);
        for (int index = 0; index < count; index++) {
            diff |= leftBytes[index] ^ rightBytes[index];
        }
        return diff == 0;
    }
}
