package dev.minevoice.common.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HmacMessageSigner {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private HmacMessageSigner() {
    }

    public static byte[] sign(byte[] message, String sharedSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(message);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign message", exception);
        }
    }

    public static boolean matches(byte[] message, byte[] signature, String sharedSecret) {
        return MessageDigest.isEqual(sign(message, sharedSecret), signature);
    }
}
