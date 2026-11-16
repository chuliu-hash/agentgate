package com.agentgate.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM，密钥由配置中的 secret 经 SHA-256 派生。
 * 密文格式：Base64(IV(12B) + CipherText + Tag)。
 */
public final class AesUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesUtils() {
    }

    public static String encrypt(String plainText, String secret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String base64CipherText, String secret) {
        try {
            byte[] data = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }

    private static SecretKeySpec key(String secret) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
