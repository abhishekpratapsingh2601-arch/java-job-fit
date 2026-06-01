package com.javajobfit.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final String version;

    public HealthController(@Value("${app.version:local}") String version) {
        this.version = version;
    }

    @GetMapping
    public Map<String, String> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", "JavaJobFit API");
        body.put("timestamp", Instant.now().toString());
        body.put("version", version);
        return Collections.unmodifiableMap(body);
    }
}
