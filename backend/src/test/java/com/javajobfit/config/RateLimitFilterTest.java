package com.javajobfit.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private MockHttpServletRequest post(String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr(ip);
        return request;
    }

    private int run(RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void allowsPostsUnderTheLimitAndRejectsBeyondIt() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 2);

        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(429);
    }

    @Test
    void rejectionBodyIsSafeJsonWithoutInternals() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);
        run(filter, post("/api/reports", "1.1.1.1"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(post("/api/reports", "1.1.1.1"), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    void extractEndpointHasItsOwnStricterBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(10, 1);

        assertThat(run(filter, post("/api/resume/extract", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/resume/extract", "1.1.1.1"))).isEqualTo(429);
        // General bucket unaffected by the extract bucket being exhausted.
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
    }

    @Test
    void differentClientIpsAreIndependent() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(429);
        assertThat(run(filter, post("/api/reports", "2.2.2.2"))).isEqualTo(200);
    }

    @Test
    void usesNearestProxyHopFromXForwardedForNotTheClientSuppliedLeftmost() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        // Our infrastructure appended 198.51.100.7; the leftmost value is whatever the client
        // sent. Both requests are the same caller and must share one bucket.
        MockHttpServletRequest first = post("/api/reports", "10.0.0.1");
        first.addHeader("X-Forwarded-For", "203.0.113.5, 198.51.100.7");
        assertThat(run(filter, first)).isEqualTo(200);

        MockHttpServletRequest second = post("/api/reports", "10.0.0.1");
        second.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.7");
        assertThat(run(filter, second)).isEqualTo(429);
    }

    @Test
    void spoofedXForwardedForCannotMintFreshRateLimitBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        // Real attack shape: the client injects its own X-Forwarded-For and rotates it on every
        // request; Cloudflare appends the true peer, so the injected values land to the left.
        // Reading the leftmost value gave every request a brand-new bucket and the limiter
        // never fired. A whole rotating chain must still collapse to one bucket.
        MockHttpServletRequest first = post("/api/reports", "10.0.0.1");
        first.addHeader("X-Forwarded-For", "1.1.1.1, 198.51.100.7");
        assertThat(run(filter, first)).isEqualTo(200);

        for (int attempt = 2; attempt <= 5; attempt++) {
            MockHttpServletRequest spoofed = post("/api/reports", "10.0.0.1");
            spoofed.addHeader("X-Forwarded-For", "9.9.9." + attempt + ", 1.1.1." + attempt + ", 198.51.100.7");
            assertThat(run(filter, spoofed)).isEqualTo(429);
        }
    }

    @Test
    void cloudflareHeaderWinsOverAnyClientSuppliedForwardedFor() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        MockHttpServletRequest first = post("/api/reports", "10.0.0.1");
        first.addHeader("CF-Connecting-IP", "203.0.113.10");
        first.addHeader("X-Forwarded-For", "1.1.1.1");
        assertThat(run(filter, first)).isEqualTo(200);

        MockHttpServletRequest sameCaller = post("/api/reports", "10.0.0.2");
        sameCaller.addHeader("CF-Connecting-IP", "203.0.113.10");
        sameCaller.addHeader("X-Forwarded-For", "2.2.2.2");
        assertThat(run(filter, sameCaller)).isEqualTo(429);

        MockHttpServletRequest otherCaller = post("/api/reports", "10.0.0.3");
        otherCaller.addHeader("CF-Connecting-IP", "203.0.113.11");
        assertThat(run(filter, otherCaller)).isEqualTo(200);
    }

    @Test
    void getAndOptionsRequestsAreNeverLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/reports/abc");
            get.setRequestURI("/api/reports/abc");
            get.setRemoteAddr("1.1.1.1");
            assertThat(run(filter, get)).isEqualTo(200);

            MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/reports");
            options.setRequestURI("/api/reports");
            options.setRemoteAddr("1.1.1.1");
            assertThat(run(filter, options)).isEqualTo(200);
        }
    }

    @Test
    void limitResetsWhenTheMinuteWindowRollsOver() throws Exception {
        long[] minute = {100L};
        RateLimitFilter filter = new RateLimitFilter(1, 1) {
            @Override
            protected long currentMinute() {
                return minute[0];
            }
        };

        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(429);

        minute[0] = 101L;
        assertThat(run(filter, post("/api/reports", "1.1.1.1"))).isEqualTo(200);
    }
}
