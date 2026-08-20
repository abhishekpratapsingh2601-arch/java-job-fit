package com.javajobfit.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private static final String SERVICE_NAME = "JavaJobFit API";
    // Supabase's free-tier idle scanner sent a pause warning (20 Aug 2026) even though the
    // keep-awake cron was issuing "select 1" through the pooler 3-4x/day — tiny reads sit
    // below its "sufficient activity" heuristic. A real INSERT is unambiguous user database
    // activity, so each keep-awake ping writes one throttled events row. The throttle exists
    // because this endpoint is an unlimited GET: without it, anyone could bloat the table.
    private static final long KEEPALIVE_MIN_INTERVAL_MS = TimeUnit.HOURS.toMillis(4);

    private final AtomicLong lastKeepaliveWriteMs = new AtomicLong(0);

    private final String version;
    private final JdbcTemplate jdbcTemplate;

    public HealthController(@Value("${app.version:local}") String version, JdbcTemplate jdbcTemplate) {
        this.version = version;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, String> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", SERVICE_NAME);
        body.put("timestamp", Instant.now().toString());
        body.put("version", version);
        return Collections.unmodifiableMap(body);
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> databaseHealth() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", SERVICE_NAME);
        body.put("timestamp", Instant.now().toString());
        body.put("version", version);

        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            body.put("status", "ok");
            body.put("database", "reachable");
            body.put("keepalive", writeThrottledKeepalive() ? "written" : "skipped");
            return ResponseEntity.ok(Collections.unmodifiableMap(body));
        } catch (RuntimeException ex) {
            body.put("status", "error");
            body.put("database", "unreachable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.unmodifiableMap(body));
        }
    }

    private boolean writeThrottledKeepalive() {
        long now = System.currentTimeMillis();
        long last = lastKeepaliveWriteMs.get();
        if (now - last < KEEPALIVE_MIN_INTERVAL_MS || !lastKeepaliveWriteMs.compareAndSet(last, now)) {
            return false;
        }
        try {
            jdbcTemplate.update(
                    "insert into events (event_name, source, created_at) values (?, ?, CURRENT_TIMESTAMP)",
                    "db_keepalive", "keep_awake");
            return true;
        } catch (RuntimeException insertFailed) {
            // Reachability is what this endpoint reports; a failed keepalive write must not
            // turn a healthy response into a 503 (that is exactly what auto-disabled the
            // keep-warm cron twice).
            lastKeepaliveWriteMs.set(last);
            return false;
        }
    }
}
