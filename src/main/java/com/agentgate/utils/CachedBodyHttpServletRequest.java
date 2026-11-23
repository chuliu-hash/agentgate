package com.agentgate.utils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体，使签名校验读取 body 后，下游 Controller 仍能再次读取。
 */
/**
 * 教学说明：包装 Servlet 请求并缓存有大小限制的 body；覆盖输入流/Reader 使验签后代理仍能读取；超限通过标志交由 Filter 拒绝。
 * 文件路径：src/main/java/com/agentgate/utils/CachedBodyHttpServletRequest.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final boolean tooLarge;

    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
        super(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        boolean overflow = false;
        try (InputStream input = request.getInputStream()) {
            int len;
            while ((len = input.read(buffer)) != -1) {
                total += len;
                if (total > maxBytes) {
                    overflow = true;
                    break;
                }
                output.write(buffer, 0, len);
            }
        }
        this.cachedBody = output.toByteArray();
        this.tooLarge = overflow;
    }

    /**
     * 返回缓存的原始请求字节，供验签与上游转发复用。
     */
    public byte[] getCachedBody() {
        return cachedBody;
    }

    /**
     * 返回缓存读取是否已发现请求体超限，供过滤器拒绝请求。
     */
    public boolean isTooLarge() {
        return tooLarge;
    }

    /**
     * 为缓存字节重新建立输入流，使后续组件可重复读取原始请求体。
     */
    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream input = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            /**
             * 判断缓存输入流是否已读取完毕。
             */
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            /**
             * 缓存字节位于内存中，读取无需等待网络可读事件。
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 设置读取监听器的兼容入口；当前包装器主要用于同步 MVC 请求。
             */
            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }

            /**
             * 从缓存输入流读取下一个字节，结束时返回 -1。
             */
            @Override
            public int read() {
                return input.read();
            }
        };
    }

    /**
     * 按请求字符集为缓存输入流创建 Reader，供字符方式读取请求体。
     */
    @Override
    public BufferedReader getReader() {
        Charset charset = StandardCharsets.UTF_8;
        String encoding = getCharacterEncoding();
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            } catch (Exception ignored) {
                // 使用 UTF-8
            }
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
