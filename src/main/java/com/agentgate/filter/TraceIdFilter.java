package com.agentgate.filter;

import com.agentgate.utils.TraceIdUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 为每个请求生成 traceId，写入 MDC（日志）与响应头，贯穿调用日志排查。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceIdUtils.generate();
        request.setAttribute(TraceIdUtils.TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        TraceIdUtils.put(traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdUtils.clear();
        }
    }
}
