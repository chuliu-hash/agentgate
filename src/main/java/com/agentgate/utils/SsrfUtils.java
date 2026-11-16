package com.agentgate.utils;

import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;

import java.net.InetAddress;
import java.net.URI;

public final class SsrfUtils {

    private SsrfUtils() {
    }

    public static void validate(String url, boolean allowLoopback) {
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
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "仅允许 http/https 上游");
        }
        if (uri.getUserInfo() != null) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址不允许包含用户信息");
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址缺少主机名");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
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
