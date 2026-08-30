package com.sentinel.revenue.execution;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RazorpayConfig {

    @Bean
    @ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
    RazorpayClient razorpayClient(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret) throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
