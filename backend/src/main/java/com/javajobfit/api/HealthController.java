package com.javajobfit.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
            return ResponseEntity.ok(Collections.unmodifiableMap(body));
        } catch (RuntimeException ex) {
            body.put("status", "error");
            body.put("database", "unreachable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.unmodifiableMap(body));
        }
    }
}
