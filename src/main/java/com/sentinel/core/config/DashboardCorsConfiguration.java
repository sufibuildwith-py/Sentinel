package com.sentinel.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DashboardCorsConfiguration implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public DashboardCorsConfiguration(
            @Value("${sentinel.cors.allowed-origins:http://localhost:3000}") String[] allowedOrigins
    ) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "X-Request-Id", "X-Correlation-ID",
                        "X-Razorpay-Signature", "X-Razorpay-Event-Id")
                .exposedHeaders("X-Request-Id", "X-Correlation-ID");
    }
}
