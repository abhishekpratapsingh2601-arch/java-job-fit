package com.javajobfit.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal in-memory, per-IP fixed-window rate limiter for the public POST endpoints.
 *
 * <p>The resume upload endpoint gets its own (stricter) bucket because Tika parsing is
 * CPU-expensive; everything else under {@code POST /api/**} shares a general bucket. GET and
 * OPTIONS (CORS preflight) requests are never limited. State is a bounded in-memory map, which is
 * the right tradeoff for a single-instance free-tier deployment — if the app ever runs on more
 * than one instance, each instance enforces its own window, which still bounds total abuse.
 */
@Component
// After the header and body-size filters, before any controller work.
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final String LIMIT_MESSAGE =
            "{\"error\":\"Too many requests. Please wait a minute and try again.\"}";

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int apiPostPerMinute;
    private final int extractPerMinute;
    private final int globalApiPostPerMinute;
    private final int globalExtractPerMinute;
    private final boolean verifyCloudflareHop;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    // Kept apart from the per-client map, which is cleared under a flood of distinct keys —
    // exactly when the global backstop must keep counting.
    private final ConcurrentHashMap<String, Window> globalWindows = new ConcurrentHashMap<>();
    private final AtomicBoolean topologyLogged = new AtomicBoolean();

    /** Per-client limits only, strict client-IP mode; used by tests. */
    public RateLimitFilter(int apiPostPerMinute, int extractPerMinute) {
        this(apiPostPerMinute, extractPerMinute, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
    }

    /** Used by tests. */
    public RateLimitFilter(int apiPostPerMinute, int extractPerMinute, int globalApiPostPerMinute, int globalExtractPerMinute) {
        this(apiPostPerMinute, extractPerMinute, globalApiPostPerMinute, globalExtractPerMinute, true);
    }

    /**
     * The global limits are a backstop that holds however client identity is forged: even a
     * caller minting a new identity per request cannot push more than this through per minute.
     * They sit far above real traffic.
     */
    @Autowired
    public RateLimitFilter(
            @Value("${app.rate-limit.api-post-per-minute:30}") int apiPostPerMinute,
            @Value("${app.rate-limit.extract-per-minute:10}") int extractPerMinute,
            @Value("${app.rate-limit.global-api-post-per-minute:600}") int globalApiPostPerMinute,
            @Value("${app.rate-limit.global-extract-per-minute:60}") int globalExtractPerMinute,
            @Value("${app.rate-limit.verify-cloudflare-hop:false}") boolean verifyCloudflareHop) {
        this.apiPostPerMinute = apiPostPerMinute;
        this.extractPerMinute = extractPerMinute;
        this.globalApiPostPerMinute = globalApiPostPerMinute;
        this.globalExtractPerMinute = globalExtractPerMinute;
        this.verifyCloudflareHop = verifyCloudflareHop;
    }

    // Every POST is limited, not just ones whose raw URI starts with "/api/": Spring routes
    // "/api;x/reports" and "//api/reports" to the same handler, so a prefix check on the raw URI
    // let a client skip the limiter by decorating the path.
    // The Razorpay webhook is exempt: it is authenticated by an HMAC signature (rejected before
    // any database work), and Razorpay delivers from a few shared addresses — limiting it would
    // let a burst of real payments, or an attacker draining the global bucket, delay unlocks.
    static final String WEBHOOK_PATH = "/api/payments/webhook";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || WEBHOOK_PATH.equals(normalizedPath(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean extract = normalizedPath(request).startsWith("/api/resume/extract");
        String bucket = extract ? "extract" : "api";
        int limit = extract ? extractPerMinute : apiPostPerMinute;
        int globalLimit = extract ? globalExtractPerMinute : globalApiPostPerMinute;
        long minute = currentMinute();

        boolean overClientLimit = increment(windows, clientKey(request) + "|" + bucket, minute) > limit;
        boolean overGlobalLimit = !overClientLimit && increment(globalWindows, bucket, minute) > globalLimit;

        if (overClientLimit || overGlobalLimit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(LIMIT_MESSAGE);
            return;
        }

        evictStaleEntriesIfNeeded(minute);
        chain.doFilter(request, response);
    }

    private static int increment(ConcurrentHashMap<String, Window> map, String key, long minute) {
        Window window = map.compute(key, (k, existing) ->
                existing == null || existing.minute != minute ? new Window(minute) : existing);
        return window.count.incrementAndGet();
    }

    /** The request path as Spring routes it: ";params" stripped and repeated slashes collapsed. */
    static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        return uri.replaceAll(";[^/]*", "").replaceAll("/{2,}", "/");
    }

    /** Overridable in tests to simulate window rollover. */
    protected long currentMinute() {
        return System.currentTimeMillis() / 60_000L;
    }

    private void evictStaleEntriesIfNeeded(long minute) {
        if (windows.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().minute != minute);
        if (windows.size() > MAX_TRACKED_KEYS) {
            // Pathological flood of distinct client keys (e.g. spoofed X-Forwarded-For values).
            // Fail open rather than let the tracking map grow without bound.
            windows.clear();
        }
    }

    /** See {@link ClientIpResolver} for the trust model. */
    private String clientKey(HttpServletRequest request) {
        logTopologyOnce(request);
        return ClientIpResolver.resolve(request, verifyCloudflareHop);
    }

    /**
     * One line per boot describing which forwarding headers production actually receives — no
     * addresses, only shape — so the trust model can be confirmed from the Render logs.
     */
    private void logTopologyOnce(HttpServletRequest request) {
        if (!topologyLogged.compareAndSet(false, true)) {
            return;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        int hops = forwarded == null || forwarded.isBlank() ? 0 : forwarded.split(",").length;
        String nearest = ClientIpResolver.nearestHop(request);
        String cloudflare = request.getHeader("CF-Connecting-IP");
        log.info("Client IP headers: CF-Connecting-IP={}, X-Forwarded-For hops={}, nearest hop trusted={}, "
                        + "nearest hop matches CF-Connecting-IP={}, peer trusted={}, strict mode={}",
                cloudflare == null ? "absent" : "present",
                hops,
                nearest == null ? "n/a" : ClientIpResolver.isTrustedInfrastructure(nearest),
                nearest != null && nearest.equals(cloudflare == null ? null : cloudflare.trim()),
                request.getRemoteAddr() != null && ClientIpResolver.isTrustedInfrastructure(request.getRemoteAddr()),
                verifyCloudflareHop);
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
