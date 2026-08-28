package com.sentinel.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DashboardCorsConfiguration implements WebMvcConfigurer {
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "X-Razorpay-Signature", "X-Razorpay-Event-Id");
    }
}
