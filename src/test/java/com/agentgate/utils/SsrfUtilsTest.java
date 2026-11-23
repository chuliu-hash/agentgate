package com.agentgate.utils;

import com.agentgate.exception.BizException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * 教学说明：JUnit 5 SSRF 地址规则测试，校验危险地址拒绝与允许场景；不是对所有网络绕过的完整证明。
 * 文件路径：src/test/java/com/agentgate/utils/SsrfUtilsTest.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

class SsrfUtilsTest {

    /**
     * 验证 loopbackIsBlockedUnlessExplicitlyAllowed 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
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

    /**
     * 验证 privateAndMetadataAddressesAreBlocked 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void privateAndMetadataAddressesAreBlocked() throws Exception {
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("10.0.0.1"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("192.168.1.10"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("172.16.0.1"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("169.254.169.254"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("100.64.0.1"), false));
    }

    /**
     * 验证 publicAddressIsAllowed 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void publicAddressIsAllowed() throws Exception {
        assertFalse(SsrfUtils.isBlocked(InetAddress.getByName("8.8.8.8"), false));
        assertFalse(SsrfUtils.isBlocked(InetAddress.getByName("223.5.5.5"), false));
    }

    /**
     * 验证 ipv6UniqueLocalIsBlocked 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void ipv6UniqueLocalIsBlocked() throws Exception {
        byte[] address = new byte[16];
        address[0] = (byte) 0xFC;
        address[15] = 0x01;
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByAddress(address), false));
    }

    /**
     * 验证 rejectsUnsupportedSchemeAndUserInfo 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void rejectsUnsupportedSchemeAndUserInfo() {
        assertThrows(BizException.class, () -> SsrfUtils.validate("file:///etc/passwd", true));
        assertThrows(BizException.class, () -> SsrfUtils.validate("ftp://example.com/file", true));
        assertThrows(BizException.class,
                () -> SsrfUtils.validate("http://user:pass@example.com/path", true));
    }
}
