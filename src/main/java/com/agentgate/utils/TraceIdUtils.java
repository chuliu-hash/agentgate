package com.agentgate.utils;

import org.slf4j.MDC;

import java.util.UUID;
/**
 * 教学说明：生成追踪 ID 并对 MDC 执行 put/clear；与 Filter、响应头、消息 traceId 配合定位请求。
 * 文件路径：src/main/java/com/agentgate/utils/TraceIdUtils.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public final class TraceIdUtils {

    public static final String TRACE_ID = "traceId";

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private TraceIdUtils() {
    }

    /**
     * 生成当前请求追踪标识，不复用用户可控制的外部追踪头。
     */
    public static String generate() {
        // 使用随机 UUID，避免复用调用方可控的追踪头作为服务端关联标识。
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 把追踪标识写入当前线程 MDC，便于日志关联。
     */
    public static void put(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * 清除当前线程 MDC 追踪信息，避免线程复用污染后续请求。
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
