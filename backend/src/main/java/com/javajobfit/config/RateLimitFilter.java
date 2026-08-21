package com.javajobfit.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final String LIMIT_MESSAGE =
            "{\"error\":\"Too many requests. Please wait a minute and try again.\"}";

    private final int apiPostPerMinute;
    private final int extractPerMinute;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.api-post-per-minute:30}") int apiPostPerMinute,
            @Value("${app.rate-limit.extract-per-minute:10}") int extractPerMinute) {
        this.apiPostPerMinute = apiPostPerMinute;
        this.extractPerMinute = extractPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !"POST".equalsIgnoreCase(request.getMethod()) || uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean extract = request.getRequestURI().startsWith("/api/resume/extract");
        int limit = extract ? extractPerMinute : apiPostPerMinute;
        long minute = currentMinute();
        String key = clientKey(request) + "|" + (extract ? "extract" : "api");

        Window window = windows.compute(key, (k, existing) ->
                existing == null || existing.minute != minute ? new Window(minute) : existing);

        if (window.count.incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(LIMIT_MESSAGE);
            return;
        }

        evictStaleEntriesIfNeeded(minute);
        chain.doFilter(request, response);
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

    /**
     * Identifies the caller for rate-limiting. Must not be attacker-controlled: reading the
     * leftmost {@code X-Forwarded-For} value let any client mint unlimited identities by
     * sending its own header, which bypassed this limiter and would bypass the free-scan caps
     * built on top of it.
     *
     * <p>Order of trust:
     * <ol>
     *   <li>{@code CF-Connecting-IP} / {@code True-Client-IP} — Cloudflare fronts
     *       {@code *.onrender.com} and overwrites these with the real peer, so a client cannot
     *       forge them.</li>
     *   <li>The <em>rightmost</em> {@code X-Forwarded-For} entry — Cloudflare appends the true
     *       peer rather than replacing the header, so anything a client injects ends up to the
     *       left of the value our own infrastructure added.</li>
     *   <li>The socket address.</li>
     * </ol>
     */
    private String clientKey(HttpServletRequest request) {
        String cloudflareIp = firstNonBlank(
                request.getHeader("CF-Connecting-IP"),
                request.getHeader("True-Client-IP"));
        if (cloudflareIp != null) {
            return cloudflareIp;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int lastComma = forwarded.lastIndexOf(',');
            String nearestHop = lastComma >= 0 ? forwarded.substring(lastComma + 1) : forwarded;
            if (!nearestHop.isBlank()) {
                return nearestHop.trim();
            }
        }

        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static final class Window {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long minute) {
            this.minute = minute;
        }
    }
}
