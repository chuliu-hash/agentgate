package com.agentgate.utils;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public final class IpUtils {

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        return getClientIp(request, false);
    }

    /**
     * @param trustProxy 只有部署在可信反向代理之后才可开启；否则客户端可伪造 X-Forwarded-For 绕过 IP 白名单
     */
    public static String getClientIp(HttpServletRequest request, boolean trustProxy) {
        if (!trustProxy) {
            return normalize(request.getRemoteAddr());
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            ip = index > 0 ? ip.substring(0, index) : ip;
            return normalize(ip.trim());
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return normalize(ip.trim());
        }
        return normalize(request.getRemoteAddr());
    }

    public static boolean isAllowed(String clientIp, String whitelist) {
        if (!StringUtils.hasText(whitelist)) {
            return true;
        }
        String[] items = whitelist.split(",");
        for (String item : items) {
            String allowed = item.trim();
            if ("*".equals(allowed)) {
                return true;
            }
            if (allowed.equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
