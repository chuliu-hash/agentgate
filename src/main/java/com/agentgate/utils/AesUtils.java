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
/**
 * 教学说明：AES/GCM/NoPadding 可逆加密：随机 12 字节 IV，SHA-256 派生密钥，Base64 存 IV+密文+Tag；用于数据库 AppSecret 保护。
 * 文件路径：src/main/java/com/agentgate/utils/AesUtils.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public final class AesUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private AesUtils() {
    }

    /**
     * 随机生成 IV，以配置派生的 AES-GCM 密钥加密；返回包含 IV、密文和认证标签的 Base64。
     */
    public static String encrypt(String plainText, String secret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            // 每次加密使用随机 IV，防止相同原文生成固定密文。
            RANDOM.nextBytes(iv);
            // 创建 AES-GCM 算法实例；解密时会同时验证认证标签，篡改或密钥错误会失败。
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

    /**
     * 从 Base64 提取 IV 并解密，同时验证 GCM 标签；密钥错误或密文被篡改时失败。
     */
    public static String decrypt(String base64CipherText, String secret) {
        try {
            byte[] data = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            // 创建 AES-GCM 算法实例；解密时会同时验证认证标签，篡改或密钥错误会失败。
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }

    /**
     * 将配置字符串用 SHA-256 派生为 256 位 AES 密钥，不是用户密码哈希存储流程。
     */
    private static SecretKeySpec key(String secret) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
