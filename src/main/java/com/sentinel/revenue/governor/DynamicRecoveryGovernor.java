package com.sentinel.revenue.governor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DynamicRecoveryGovernor {
    public static final String VERSION = "dynamic-governor-v1";

    public DynamicGovernorAssessment assess(GovernorSignalSnapshot snapshot, double maximumToolFailureRate) {
        double toolRatio = ratio(snapshot.toolFailureRate(), maximumToolFailureRate);
        double unreconciledRatio = ratio(snapshot.unreconciledValueMinor(), snapshot.maximumUnreconciledValueMinor());
        double exposureRatio = ratio(snapshot.activeExposureMinor(), snapshot.maximumExposureMinor());
        double pressure = Math.max(toolRatio, Math.max(unreconciledRatio, exposureRatio));
        GovernorPosture posture;
        double multiplier;
        if (pressure >= 1.5) { posture = GovernorPosture.RED; multiplier = 0; }
        else if (pressure >= 1.0) { posture = GovernorPosture.ORANGE; multiplier = 0; }
        else if (pressure >= 0.8) { posture = GovernorPosture.YELLOW; multiplier = 0.1; }
        else { posture = GovernorPosture.GREEN; multiplier = 1; }
        return new DynamicGovernorAssessment(posture, multiplier, List.of(
                "toolFailureRate=" + snapshot.toolFailureRate() + " limit=" + maximumToolFailureRate,
                "unreconciledValueMinor=" + snapshot.unreconciledValueMinor()
                        + " limit=" + snapshot.maximumUnreconciledValueMinor(),
                "activeExposureMinor=" + snapshot.activeExposureMinor()
                        + " limit=" + snapshot.maximumExposureMinor()), VERSION);
    }

    private double ratio(double actual, double maximum) {
        if (maximum <= 0) return actual > 0 ? Double.POSITIVE_INFINITY : 0;
        return actual / maximum;
    }
}
