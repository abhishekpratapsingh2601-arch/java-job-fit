package com.javajobfit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.Reader;

import javax.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** The parts MockMvc cannot exercise: bodies that declare no length (chunked encoding). */
class RequestBodyLimitFilterTest {
    private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter();

    /** A chunked request: the body is there but no Content-Length is declared. */
    private MockHttpServletRequest chunked(String contentType, int bytes) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reports") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.setContentType(contentType);
        request.setContent(new byte[bytes]);
        return request;
    }

    private void drainStream(javax.servlet.ServletRequest request) throws IOException {
        byte[] buffer = new byte[8192];
        while (request.getInputStream().read(buffer) != -1) {
            // keep reading
        }
    }

    private void drainReader(javax.servlet.ServletRequest request) throws IOException {
        try (Reader reader = request.getReader()) {
            char[] buffer = new char[8192];
            while (reader.read(buffer) != -1) {
                // keep reading
            }
        }
    }

    @Test
    void aChunkedBodyIsCutOffOnceItPassesTheCap() {
        FilterChain readsStream = (request, response) -> drainStream(request);
        FilterChain readsReader = (request, response) -> drainReader(request);
        int tooBig = (int) RequestBodyLimitFilter.MAX_BODY_BYTES + 1;

        assertThatThrownBy(() -> filter.doFilter(chunked("application/json", tooBig), new MockHttpServletResponse(), readsStream))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> filter.doFilter(chunked("application/json", tooBig), new MockHttpServletResponse(), readsReader))
                .isInstanceOf(IOException.class);
        // The content type is not what decides it: text/plain (the webhook's raw String body) too.
        assertThatThrownBy(() -> filter.doFilter(chunked("text/plain", tooBig), new MockHttpServletResponse(), readsStream))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> filter.doFilter(chunked(null, tooBig), new MockHttpServletResponse(), readsStream))
                .isInstanceOf(IOException.class);
    }

    @Test
    void aChunkedBodyUnderTheCapReadsNormally() throws Exception {
        FilterChain readsStream = (request, response) -> drainStream(request);

        filter.doFilter(chunked("application/json", (int) RequestBodyLimitFilter.MAX_BODY_BYTES),
                new MockHttpServletResponse(), readsStream);
    }

    @Test
    void onlyRealMultipartUploadsSkipTheCap() {
        MockHttpServletRequest multipart = new MockHttpServletRequest("POST", "/api/resume/extract");
        multipart.setContentType("Multipart/Form-Data; boundary=x");
        MockHttpServletRequest json = new MockHttpServletRequest("POST", "/api/reports");
        json.setContentType("application/json; charset=multipart");

        assertThat(filter.shouldNotFilter(multipart)).isTrue();
        assertThat(filter.shouldNotFilter(json)).isFalse();
    }
}
