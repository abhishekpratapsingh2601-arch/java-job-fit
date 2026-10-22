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
    void cloudflareHeaderIsTrustedWhenACloudflareEdgeDeliveredTheRequest() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        // Through Cloudflare: the nearest hop is a Cloudflare edge, and Cloudflare itself sets
        // CF-Connecting-IP (it rejects requests that already carry one).
        MockHttpServletRequest first = post("/api/reports", "10.0.0.1");
        first.addHeader("CF-Connecting-IP", "203.0.113.10");
        first.addHeader("X-Forwarded-For", "1.1.1.1, 162.158.10.20");
        assertThat(run(filter, first)).isEqualTo(200);

        MockHttpServletRequest sameCaller = post("/api/reports", "10.0.0.2");
        sameCaller.addHeader("CF-Connecting-IP", "203.0.113.10");
        sameCaller.addHeader("X-Forwarded-For", "2.2.2.2, 172.68.1.1");
        assertThat(run(filter, sameCaller)).isEqualTo(429);

        MockHttpServletRequest otherCaller = post("/api/reports", "10.0.0.3");
        otherCaller.addHeader("CF-Connecting-IP", "203.0.113.11");
        otherCaller.addHeader("X-Forwarded-For", "162.158.10.20");
        assertThat(run(filter, otherCaller)).isEqualTo(200);
    }

    @Test
    void aForgedCloudflareHeaderSentAroundCloudflareIsIgnored() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        // Straight to the origin: the delivering hop is the attacker's own public address, so the
        // rotating CF-Connecting-IP it sends is not believed.
        for (int attempt = 1; attempt <= 4; attempt++) {
            MockHttpServletRequest forged = post("/api/reports", "10.0.0.1");
            forged.addHeader("CF-Connecting-IP", "203.0.113." + attempt);
            forged.addHeader("True-Client-IP", "198.51.100." + attempt);
            forged.addHeader("X-Forwarded-For", "45.33.32.156");
            assertThat(run(filter, forged)).isEqualTo(attempt == 1 ? 200 : 429);
        }

        // Same when there is no X-Forwarded-For and the socket peer is the attacker.
        RateLimitFilter direct = new RateLimitFilter(1, 1);
        for (int attempt = 1; attempt <= 3; attempt++) {
            MockHttpServletRequest forged = post("/api/reports", "45.33.32.157");
            forged.addHeader("CF-Connecting-IP", "203.0.113." + attempt);
            assertThat(run(direct, forged)).isEqualTo(attempt == 1 ? 200 : 429);
        }
    }

    @Test
    void cloudflareHeaderIsStillTrustedBehindPrivateInfrastructure() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        // If the platform's own internal proxy is the nearest hop, behave exactly as before:
        // distinct real clients must keep distinct buckets.
        MockHttpServletRequest a = post("/api/reports", "10.0.0.1");
        a.addHeader("CF-Connecting-IP", "203.0.113.20");
        a.addHeader("X-Forwarded-For", "203.0.113.20, 10.20.30.40");
        MockHttpServletRequest b = post("/api/reports", "10.0.0.1");
        b.addHeader("CF-Connecting-IP", "203.0.113.21");
        b.addHeader("X-Forwarded-For", "203.0.113.21, 10.20.30.40");

        assertThat(run(filter, a)).isEqualTo(200);
        assertThat(run(filter, b)).isEqualTo(200);
    }

    @Test
    void theGlobalBackstopHoldsEvenWhenEveryRequestLooksLikeANewClient() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 100, 3, 1);

        for (int i = 1; i <= 3; i++) {
            assertThat(run(filter, post("/api/reports", "45.33.32." + i))).isEqualTo(200);
        }
        assertThat(run(filter, post("/api/reports", "45.33.32.99"))).isEqualTo(429);

        assertThat(run(filter, post("/api/resume/extract", "45.33.32.50"))).isEqualTo(200);
        assertThat(run(filter, post("/api/resume/extract", "45.33.32.51"))).isEqualTo(429);
    }

    @Test
    void recognisesCloudflareAndPrivateRangesButNothingElse() {
        assertThat(ClientIpResolver.isTrustedInfrastructure("162.158.10.20")).isTrue();
        assertThat(ClientIpResolver.isTrustedInfrastructure("104.23.255.255")).isTrue();
        assertThat(ClientIpResolver.isTrustedInfrastructure("2606:4700:10::6816:1")).isTrue();
        assertThat(ClientIpResolver.isTrustedInfrastructure("10.1.2.3")).isTrue();
        assertThat(ClientIpResolver.isTrustedInfrastructure("100.64.0.1")).isTrue();
        assertThat(ClientIpResolver.isTrustedInfrastructure("127.0.0.1")).isTrue();
        assertThat(ClientIpResolver.isTrustedInfrastructure("162.158.10.20:443")).isTrue();

        assertThat(ClientIpResolver.isTrustedInfrastructure("104.32.0.1")).isFalse();
        assertThat(ClientIpResolver.isTrustedInfrastructure("1.1.1.1")).isFalse();
        assertThat(ClientIpResolver.isTrustedInfrastructure("216.24.57.18")).isFalse();
        assertThat(ClientIpResolver.isTrustedInfrastructure("2001:db8::1")).isFalse();
        // Hostnames are never resolved, so a header value cannot trigger a DNS lookup.
        assertThat(ClientIpResolver.isTrustedInfrastructure("localhost")).isFalse();
        assertThat(ClientIpResolver.isTrustedInfrastructure("cloudflare.com")).isFalse();
        assertThat(ClientIpResolver.isTrustedInfrastructure("not an ip")).isFalse();
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

    @Test
    void decoratingThePathCannotSkipTheLimiter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);

        // Spring routes all of these to the same handlers, so they must share the same buckets.
        assertThat(run(filter, post("/api;x=1/reports", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("//api/reports", "1.1.1.1"))).isEqualTo(429);

        assertThat(run(filter, post("/api/resume;x=1/extract", "1.1.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api//resume/extract", "1.1.1.1"))).isEqualTo(429);
    }

    @Test
    void theSignedPaymentWebhookIsNeverRateLimitedButEverythingElseIs() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1, 1, 1);

        for (int i = 0; i < 5; i++) {
            assertThat(run(filter, post("/api/payments/webhook", "52.66.1.1"))).isEqualTo(200);
            assertThat(run(filter, post("/api;x=1/payments/webhook", "52.66.1.1"))).isEqualTo(200);
        }
        // Only that exact route: its neighbours and look-alikes are still limited.
        assertThat(run(filter, post("/api/payments/link", "52.66.1.1"))).isEqualTo(200);
        assertThat(run(filter, post("/api/payments/link", "52.66.1.1"))).isEqualTo(429);
        assertThat(run(filter, post("/api/payments/webhook/x", "52.66.1.2"))).isEqualTo(429);
    }

    @Test
    void defaultModeKeysOnCloudflareHeaderExactlyAsBeforeSoRealUsersAreNeverMerged() throws Exception {
        // Unknown platform topology: even if every request's nearest hop were one shared public
        // address, distinct CF-Connecting-IP clients keep distinct buckets in the default mode.
        RateLimitFilter filter = new RateLimitFilter(1, 1, Integer.MAX_VALUE, Integer.MAX_VALUE, false);

        MockHttpServletRequest a = post("/api/reports", "10.0.0.1");
        a.addHeader("CF-Connecting-IP", "203.0.113.30");
        a.addHeader("X-Forwarded-For", "203.0.113.30, 35.190.1.1");
        MockHttpServletRequest b = post("/api/reports", "10.0.0.1");
        b.addHeader("CF-Connecting-IP", "203.0.113.31");
        b.addHeader("X-Forwarded-For", "203.0.113.31, 35.190.1.1");
        assertThat(run(filter, a)).isEqualTo(200);
        assertThat(run(filter, b)).isEqualTo(200);

        // True-Client-IP is never a key, in either mode.
        MockHttpServletRequest c = post("/api/reports", "45.33.32.200");
        c.addHeader("True-Client-IP", "198.51.100.1");
        MockHttpServletRequest d = post("/api/reports", "45.33.32.200");
        d.addHeader("True-Client-IP", "198.51.100.2");
        assertThat(run(filter, c)).isEqualTo(200);
        assertThat(run(filter, d)).isEqualTo(429);
    }
}
