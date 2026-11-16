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

    public byte[] getCachedBody() {
        return cachedBody;
    }

    public boolean isTooLarge() {
        return tooLarge;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream input = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }

            @Override
            public int read() {
                return input.read();
            }
        };
    }

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
