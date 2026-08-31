package com.sentinel.revenue.communication;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class SimulationCommunicationAdapter implements CommunicationAdapter {
    @Override public CommunicationResult send(String customerReference, String channel, String templateId) {
        return new CommunicationResult(mode(), "SIMULATED_NOT_SENT", "sim_" + digest(customerReference + templateId));
    }
    @Override public String mode() { return "TEST_SIMULATION"; }
    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)), 0, 6); }
        catch (Exception impossible) { throw new IllegalStateException("Simulation reference unavailable"); }
    }
}
