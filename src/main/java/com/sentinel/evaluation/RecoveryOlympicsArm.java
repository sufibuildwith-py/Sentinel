package com.sentinel.evaluation;

public enum RecoveryOlympicsArm {
    NO_INTERVENTION("A", "No intervention", "CONTROL"),
    BLIND_INTERVENTION("B", "Blind/static intervention baseline", "SYNTHETIC_BASELINE"),
    STATIC_RULES("C", "Static rules", "SYNTHETIC_BASELINE"),
    RECOVERAX_APPROXIMATION("D", "RecoveraX-style EV approximation", "DOCUMENTED_APPROXIMATION"),
    RIE_APPROXIMATION("E", "RIE-style timing approximation", "DOCUMENTED_APPROXIMATION"),
    SENTINEL_BASELINE("F", "Sentinel current baseline", "SENTINEL_BASELINE"),
    SENTINEL_V2("G", "Sentinel V2", "SENTINEL_V2");

    private final String code;
    private final String label;
    private final String methodologyLabel;

    RecoveryOlympicsArm(String code, String label, String methodologyLabel) {
        this.code = code;
        this.label = label;
        this.methodologyLabel = methodologyLabel;
    }

    public String code() { return code; }
    public String label() { return label; }
    public String methodologyLabel() { return methodologyLabel; }
}
