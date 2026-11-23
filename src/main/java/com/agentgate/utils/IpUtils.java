package com.agentgate.utils;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
/**
 * 教学说明：取得客户端 IP 并匹配白名单；默认使用远端地址，仅信任代理配置打开时使用转发头；支持精确 IP 与 *，不支持 CIDR。
 * 文件路径：src/main/java/com/agentgate/utils/IpUtils.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public final class IpUtils {

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private IpUtils() {
    }

    /**
     * 默认取 TCP 远端地址；显式信任代理时才采用转发头，防止随意伪造白名单来源。
     */
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

    /**
     * 匹配应用配置的 IP 白名单；支持精确地址与通配符，不支持 CIDR 网段。
     */
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

    /**
     * 规范化客户端地址文本，供后续白名单匹配。
     */
    private static String normalize(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
