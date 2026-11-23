package com.agentgate.utils;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
/**
 * 教学说明：JUnit 5 加密工具回归测试；对照每个 Test 的输入、调用与断言理解实际覆盖范围，不等同密钥管理安全审计。
 * 文件路径：src/test/java/com/agentgate/utils/AesUtilsTest.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

class AesUtilsTest {

    private static final String SECRET = "AgentGate@2026SecretKey32Bytes!!";

    /**
     * 验证同一密钥加密后可解密恢复原文。
     */
    @Test
    void encryptAndDecryptRoundTrip() {
        String plain = "app-secret-0123456789abcdef";
        String cipher = AesUtils.encrypt(plain, SECRET);

        assertNotEquals(plain, cipher);
        assertEquals(plain, AesUtils.decrypt(cipher, SECRET));
    }

    /**
     * 验证随机 IV 使相同输入的两次密文不同。
     */
    @Test
    void randomIvProducesDifferentCipherText() {
        String plain = "same-plain-text";
        assertNotEquals(AesUtils.encrypt(plain, SECRET), AesUtils.encrypt(plain, SECRET));
    }

    /**
     * 验证密文篡改导致 GCM 校验失败。
     */
    @Test
    void tamperedCipherTextFails() {
        byte[] data = Base64.getDecoder().decode(AesUtils.encrypt("secret", SECRET));
        data[data.length - 1] ^= 0x01;

        assertThrows(IllegalStateException.class,
                () -> AesUtils.decrypt(Base64.getEncoder().encodeToString(data), SECRET));
    }

    /**
     * 验证错误主密钥不能解密密文。
     */
    @Test
    void wrongKeyFails() {
        String cipher = AesUtils.encrypt("secret", SECRET);
        assertThrows(IllegalStateException.class,
                () -> AesUtils.decrypt(cipher, "another-secret-key"));
    }
}
