package com.example.apitool.config;

import com.example.apitool.audit.AuditProperties;
import com.example.apitool.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RateLimitProperties.class, AuditProperties.class})
public class AppConfig {
}
