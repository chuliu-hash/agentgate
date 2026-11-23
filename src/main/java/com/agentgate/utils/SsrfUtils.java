package com.agentgate.utils;

import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;

import java.net.InetAddress;
import java.net.URI;
/**
 * 教学说明：验证 URL 协议、userinfo、主机解析地址并拒绝危险网络；允许 loopback 是演示开关；校验不等于固定实际连接 IP。
 * 文件路径：src/main/java/com/agentgate/utils/SsrfUtils.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public final class SsrfUtils {

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private SsrfUtils() {
    }

    /**
     * 检查上游 URL 协议、凭据及解析地址，拦截危险网络；不能保证检查 IP 与实际连接 IP 永远相同。
     */
    public static void validate(String url, boolean allowLoopback) {
        // 空地址无法安全解析出目标主机，按 SSRF 风险拒绝。
        if (url == null || url.trim().isEmpty()) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址格式非法");
        }
        String scheme = uri.getScheme();
        // 仅允许 HTTP(S)，避免通过其他 URI scheme 访问本地文件或非 HTTP 服务。
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "仅允许 http/https 上游");
        }
        // URL userinfo 容易造成目标主机判断歧义，不允许携带用户名或密码。
        if (uri.getUserInfo() != null) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址不允许包含用户信息");
        }
        String host = uri.getHost();
        // 必须能解析出主机名，后续才能检查其解析到的所有地址。
        if (host == null || host.isEmpty()) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址缺少主机名");
        }
        try {
            // 检查主机的全部解析结果，避免仅检查一个地址导致 DNS 多地址绕过。
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                // 主机只要有一个地址落入受限网段就拒绝，避免通过多地址解析绕过检查。
                if (isBlocked(address, allowLoopback)) {
                    throw new BizException(ErrorCode.SSRF_BLOCKED,
                            "上游地址指向受限网络: " + address.getHostAddress());
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游域名解析失败");
        }
    }

    /** 判断地址是否属于禁止访问的本机、链路本地、私网、组播或特殊用途网段。 */
    /** 判断地址是否落入本机、私网、链路本地或其他禁止访问的特殊网段。 */
    static boolean isBlocked(InetAddress address, boolean allowLoopback) {
        if (address.isLoopbackAddress()) {
            return !allowLoopback;
        }
        if (address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            // 0.0.0.0/8
            if (first == 0) {
                return true;
            }
            // 100.64.0.0/10 运营商级 NAT
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 169.254.0.0/16 链路本地（含云元数据 169.254.169.254）
            return first == 169 && second == 254;
        }
        if (bytes.length == 16) {
            // fc00::/7 唯一本地地址
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return false;
    }
}
