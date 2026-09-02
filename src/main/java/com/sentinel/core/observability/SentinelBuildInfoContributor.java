package com.sentinel.core.observability;

import com.sentinel.revenue.execution.RazorpayProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SentinelBuildInfoContributor implements InfoContributor {
    private final ObjectProvider<BuildProperties> buildProperties;
    private final RazorpayProperties razorpay;
    private final String commit;

    public SentinelBuildInfoContributor(ObjectProvider<BuildProperties> buildProperties,
                                        RazorpayProperties razorpay,
                                        @Value("${RAILWAY_GIT_COMMIT_SHA:${GIT_COMMIT:unknown}}") String commit) {
        this.buildProperties = buildProperties;
        this.razorpay = razorpay;
        this.commit = commit == null || commit.isBlank() ? "unknown" : commit;
    }

    @Override
    public void contribute(Info.Builder builder) {
        BuildProperties build = buildProperties.getIfAvailable();
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("name", build == null ? "sentinel" : build.getName());
        application.put("version", build == null ? "development" : build.getVersion());
        application.put("commit", commit);
        if (build != null && build.getTime() != null) application.put("buildTime", build.getTime());
        builder.withDetail("application", application);
        boolean credentialsConfigured = razorpay.keyId().startsWith("rzp_test_") && !razorpay.keySecret().isBlank();
        builder.withDetail("providerExecution", Map.of(
                "provider", "RAZORPAY",
                "mode", "TEST",
                "configured", credentialsConfigured,
                "enabled", razorpay.enabled(),
                "credentialsConfigured", credentialsConfigured,
                "status", razorpay.enabled() ? "READY" : "DISABLED",
                "reason", razorpay.enabled() ? "Test Mode provider execution is enabled" : "Razorpay Test Mode execution is disabled"
        ));
    }
}
