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
    void usesFirstXForwardedForValueWhenPresent() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        MockHttpServletRequest first = post("/api/reports", "10.0.0.1");
        first.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        assertThat(run(filter, first)).isEqualTo(200);

        MockHttpServletRequest second = post("/api/reports", "10.0.0.2");
        second.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.2");
        assertThat(run(filter, second)).isEqualTo(429);
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
