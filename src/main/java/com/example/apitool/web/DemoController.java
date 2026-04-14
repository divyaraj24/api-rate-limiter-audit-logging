package com.example.apitool.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "api-tool",
                "status", "ok",
                "time", Instant.now().toString()
        ));
    }

    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> reports() {
        return ResponseEntity.ok(Map.of(
                "message", "Premium endpoint simulation",
                "generatedAt", Instant.now().toString()
        ));
    }
}
