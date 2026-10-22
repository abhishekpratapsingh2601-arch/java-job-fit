package com.javajobfit.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * Works out which client a request came from, for rate limiting.
 *
 * <p>Verified against production on 22 Oct 2026: Cloudflare fronts {@code *.onrender.com} and
 * rejects any request that already carries {@code CF-Connecting-IP} (error 1000), so on the
 * public path that header is always Cloudflare's own; forged {@code True-Client-IP} and
 * {@code X-Forwarded-For} values did not create new buckets either. {@code True-Client-IP} is
 * passed through from clients untouched, so it is never trusted.
 *
 * <p>The remaining risk is a request sent straight to Render's origin, around Cloudflare, with a
 * forged {@code CF-Connecting-IP}. <b>Strict mode</b> closes it: the header is believed only when
 * the hop that delivered the request (rightmost {@code X-Forwarded-For} entry, else the socket
 * peer) is a Cloudflare edge or private infrastructure; otherwise that hop is the key. Strict mode
 * is off by default because if Render's proxy chain ended in one of Render's own public addresses,
 * every real user would share that one key. Turn it on only after the boot log line from
 * {@link RateLimitFilter} shows "nearest hop trusted=true" (or "matches CF-Connecting-IP=true").
 * The global per-minute cap bounds abuse in either mode.
 */
final class ClientIpResolver {
    /** https://www.cloudflare.com/ips/ — fetched 22 Oct 2026. */
    private static final List<String> CLOUDFLARE_RANGES = Arrays.asList(
            "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
            "141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
            "197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
            "104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
            "2400:cb00::/32", "2606:4700::/32", "2803:f800::/32", "2405:b500::/32",
            "2405:8100::/32", "2a06:98c0::/29", "2c0f:f248::/32");

    private static final Pattern IPV4_LITERAL = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

    private ClientIpResolver() {
    }

    static String resolve(HttpServletRequest request, boolean strict) {
        String deliveringHop = nearestForwardedHop(request.getHeader("X-Forwarded-For"));
        if (deliveringHop == null) {
            deliveringHop = blankToNull(request.getRemoteAddr());
        }
        String cloudflareClient = blankToNull(request.getHeader("CF-Connecting-IP"));

        boolean believeCloudflare = cloudflareClient != null
                && (!strict || deliveringHop == null || isTrustedInfrastructure(deliveringHop));
        if (believeCloudflare) {
            return cloudflareClient;
        }
        return deliveringHop == null ? "unknown" : deliveringHop;
    }

    static String nearestHop(HttpServletRequest request) {
        return nearestForwardedHop(request.getHeader("X-Forwarded-For"));
    }

    /** True for Cloudflare edges and private/loopback hops, which only our own infrastructure uses. */
    static boolean isTrustedInfrastructure(String ip) {
        InetAddress address = parseLiteral(ip);
        if (address == null) {
            return false;
        }
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || inRange(address, "100.64.0.0/10") || inRange(address, "fc00::/7")) {
            return true;
        }
        for (String range : CLOUDFLARE_RANGES) {
            if (inRange(address, range)) {
                return true;
            }
        }
        return false;
    }

    /** Rightmost entry: anything a client injects lands to its left. */
    private static String nearestForwardedHop(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        int lastComma = forwarded.lastIndexOf(',');
        return blankToNull(lastComma >= 0 ? forwarded.substring(lastComma + 1) : forwarded);
    }

    /** Parses only IP literals — never a hostname, so this can never trigger a DNS lookup. */
    private static InetAddress parseLiteral(String value) {
        String ip = value.trim();
        if (ip.startsWith("[") && ip.indexOf(']') > 0) {
            ip = ip.substring(1, ip.indexOf(']'));
        } else if (ip.indexOf(':') == ip.lastIndexOf(':') && ip.indexOf(':') > 0 && ip.contains(".")) {
            ip = ip.substring(0, ip.indexOf(':')); // IPv4 with a port
        }
        boolean literal = IPV4_LITERAL.matcher(ip).matches() || (ip.contains(":") && IPV6_LITERAL.matcher(ip).matches());
        if (!literal) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean inRange(InetAddress address, String cidr) {
        int slash = cidr.indexOf('/');
        InetAddress network = parseLiteral(cidr.substring(0, slash));
        int prefix = Integer.parseInt(cidr.substring(slash + 1));
        byte[] a = address.getAddress();
        byte[] n = network == null ? null : network.getAddress();
        if (n == null || a.length != n.length) {
            return false;
        }
        int fullBytes = prefix / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (a[i] != n[i]) {
                return false;
            }
        }
        int remainingBits = prefix % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = (0xFF << (8 - remainingBits)) & 0xFF;
        return (a[fullBytes] & mask) == (n[fullBytes] & mask);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
