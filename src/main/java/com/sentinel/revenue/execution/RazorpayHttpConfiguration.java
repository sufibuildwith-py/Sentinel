package com.sentinel.revenue.execution;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class RazorpayHttpConfiguration {
    @Bean("razorpayHttpClient")
    HttpClient razorpayHttpClient(RazorpayProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    }
}
