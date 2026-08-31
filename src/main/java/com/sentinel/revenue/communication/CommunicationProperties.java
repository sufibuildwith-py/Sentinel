package com.sentinel.revenue.communication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.LocalTime;
import java.util.Set;
@ConfigurationProperties(prefix = "sentinel.communication")
public record CommunicationProperties(LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                      int maxContactsPer24Hours, Set<String> allowedTemplates,
                                      int maximumPromiseDays) {
    public CommunicationProperties { allowedTemplates = Set.copyOf(allowedTemplates); }
}
