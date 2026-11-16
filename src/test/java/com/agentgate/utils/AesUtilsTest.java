package com.agentgate.utils;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesUtilsTest {

    private static final String SECRET = "AgentGate@2026SecretKey32Bytes!!";

    @Test
    void encryptAndDecryptRoundTrip() {
        String plain = "app-secret-0123456789abcdef";
        String cipher = AesUtils.encrypt(plain, SECRET);

        assertNotEquals(plain, cipher);
        assertEquals(plain, AesUtils.decrypt(cipher, SECRET));
    }

    @Test
    void randomIvProducesDifferentCipherText() {
        String plain = "same-plain-text";
        assertNotEquals(AesUtils.encrypt(plain, SECRET), AesUtils.encrypt(plain, SECRET));
    }

    @Test
    void tamperedCipherTextFails() {
        byte[] data = Base64.getDecoder().decode(AesUtils.encrypt("secret", SECRET));
        data[data.length - 1] ^= 0x01;

        assertThrows(IllegalStateException.class,
                () -> AesUtils.decrypt(Base64.getEncoder().encodeToString(data), SECRET));
    }

    @Test
    void wrongKeyFails() {
        String cipher = AesUtils.encrypt("secret", SECRET);
        assertThrows(IllegalStateException.class,
                () -> AesUtils.decrypt(cipher, "another-secret-key"));
    }
}
