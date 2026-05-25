package com.javajobfit.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> root() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("name", "JavaJobFit API");
        response.put("status", "ok");
        response.put("health", "/api/health");
        return response;
    }
}
