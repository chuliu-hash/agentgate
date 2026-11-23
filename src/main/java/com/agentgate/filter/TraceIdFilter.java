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
 * 为每个请求生成服务端 traceId，并写入 request attribute、响应头和 MDC。
 * 高优先级保证后续过滤器和 MVC 日志能够使用同一标识；请求结束时清除线程上下文。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** 建立 traceId 上下文并继续过滤链；无论下游成功或抛出异常都清理 MDC。 */
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
