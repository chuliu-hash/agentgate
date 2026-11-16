package com.agentgate.utils;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdUtils {

    public static final String TRACE_ID = "traceId";

    private TraceIdUtils() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void put(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
