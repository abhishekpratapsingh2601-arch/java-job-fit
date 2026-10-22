package com.javajobfit.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps non-multipart request bodies before Jackson reads them.
 *
 * <p>Bean validation ({@code @Size}) only runs after the whole body has been parsed into
 * strings, so without this one 100MB POST could exhaust the 192MB heap and take the single
 * instance down. The largest legitimate body is a report request: two 12,000-character fields,
 * at most ~72KB in UTF-8. Multipart uploads are excluded — they have their own 5MB/6MB limits.
 */
@Component
// Before anything can read the body.
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestBodyLimitFilter extends OncePerRequestFilter {
    static final long MAX_BODY_BYTES = 128L * 1024L;
    private static final String TOO_LARGE_MESSAGE = "{\"error\":\"Request is too large.\"}";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(TOO_LARGE_MESSAGE);
            return;
        }
        // Chunked bodies declare no length, so count bytes as they are read instead.
        chain.doFilter(new BoundedRequest(request), response);
    }

    private static final class BoundedRequest extends HttpServletRequestWrapper {
        private ServletInputStream bounded;

        BoundedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (bounded == null) {
                bounded = new BoundedInputStream(super.getInputStream());
            }
            return bounded;
        }

        // The default wrapper hands out the container's own reader, which would skip the count.
        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class BoundedInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private long count;

        BoundedInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = delegate.read(buffer, offset, length);
            if (read > 0) {
                count(read);
            }
            return read;
        }

        private void count(int bytes) throws IOException {
            count += bytes;
            if (count > MAX_BODY_BYTES) {
                throw new IOException("Request body exceeds " + MAX_BODY_BYTES + " bytes");
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
