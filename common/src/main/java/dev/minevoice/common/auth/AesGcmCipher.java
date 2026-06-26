package dev.minevoice.common.auth;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public final class AesGcmCipher {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesGcmCipher() {}

    public static byte[] encrypt(byte[] key, byte[] plaintext) {
        if (key == null || key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes for AES-128");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);
            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt payload", e);
        }
    }

    public static byte[] decrypt(byte[] key, byte[] encrypted) {
        if (key == null || key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes for AES-128");
        }
        if (encrypted.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("Encrypted payload too short");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            
            return cipher.doFinal(encrypted, iv.length, encrypted.length - iv.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt payload", e);
        }
    }
}
