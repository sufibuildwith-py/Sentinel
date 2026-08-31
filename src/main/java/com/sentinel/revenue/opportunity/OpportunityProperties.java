package com.sentinel.revenue.opportunity;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "sentinel.opportunity")
public record OpportunityProperties(CausalMaturity maturity, String mode) { }
