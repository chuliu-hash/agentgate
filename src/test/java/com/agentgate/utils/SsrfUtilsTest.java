package com.agentgate.utils;

import com.agentgate.exception.BizException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrfUtilsTest {

    @Test
    void loopbackIsBlockedUnlessExplicitlyAllowed() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertTrue(SsrfUtils.isBlocked(loopback, false));
        assertFalse(SsrfUtils.isBlocked(loopback, true));

        assertThrows(BizException.class,
                () -> SsrfUtils.validate("http://127.0.0.1:8081/mock-api/weather", false));
        assertDoesNotThrow(
                () -> SsrfUtils.validate("http://127.0.0.1:8081/mock-api/weather", true));
    }

    @Test
    void privateAndMetadataAddressesAreBlocked() throws Exception {
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("10.0.0.1"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("192.168.1.10"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("172.16.0.1"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("169.254.169.254"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("100.64.0.1"), false));
    }

    @Test
    void publicAddressIsAllowed() throws Exception {
        assertFalse(SsrfUtils.isBlocked(InetAddress.getByName("8.8.8.8"), false));
        assertFalse(SsrfUtils.isBlocked(InetAddress.getByName("223.5.5.5"), false));
    }

    @Test
    void ipv6UniqueLocalIsBlocked() throws Exception {
        byte[] address = new byte[16];
        address[0] = (byte) 0xFC;
        address[15] = 0x01;
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByAddress(address), false));
    }

    @Test
    void rejectsUnsupportedSchemeAndUserInfo() {
        assertThrows(BizException.class, () -> SsrfUtils.validate("file:///etc/passwd", true));
        assertThrows(BizException.class, () -> SsrfUtils.validate("ftp://example.com/file", true));
        assertThrows(BizException.class,
                () -> SsrfUtils.validate("http://user:pass@example.com/path", true));
    }
}
