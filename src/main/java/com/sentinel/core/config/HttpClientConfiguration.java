package com.sentinel.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class HttpClientConfiguration {

    @Bean
    HttpClient geminiHttpClient(GeminiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
    }
}
