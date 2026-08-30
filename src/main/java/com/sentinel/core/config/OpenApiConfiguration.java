package com.sentinel.core.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(
        title = "Sentinel API",
        version = "1.0",
        description = "Governed multi-agent revenue recovery platform"
))
public class OpenApiConfiguration {
}
