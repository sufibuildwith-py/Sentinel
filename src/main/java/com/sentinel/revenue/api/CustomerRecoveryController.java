package com.sentinel.revenue.api;

import com.sentinel.revenue.communication.*;
import com.sentinel.revenue.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue/customer-recovery")
public class CustomerRecoveryController {
    private final GovernedCommunicationService communication;
    private final PromiseToPayService promises;
    private final CustomerIntentValidator intents;
    public CustomerRecoveryController(GovernedCommunicationService communication,
                                      PromiseToPayService promises, CustomerIntentValidator intents) {
        this.communication = communication; this.promises = promises; this.intents = intents;
    }
    @PostMapping("/communications")
    public ResponseEntity<CustomerInteraction> communicate(@RequestBody CommunicationRequest request) {
        return ResponseEntity.ok(communication.communicate(request.incidentId(), request.actionId(),
                request.customerRef(), request.channel(), intents.validate(request.intent()),
                request.templateId(), Instant.now()));
    }
    @PostMapping("/promises")
    public ResponseEntity<PromiseToPay> promise(@RequestBody PromiseRequest request) {
        return ResponseEntity.ok(promises.create(request.incidentId(), request.actionId(),
                request.customerRef(), request.promisedAmountMinor(), request.balanceMinor(),
                request.dueAt(), request.ambiguous(), Instant.now()));
    }
    public record CommunicationRequest(UUID incidentId, UUID actionId, String customerRef,
                                       String channel, String intent, String templateId) { }
    public record PromiseRequest(UUID incidentId, UUID actionId, String customerRef,
                                 long promisedAmountMinor, long balanceMinor,
                                 Instant dueAt, boolean ambiguous) { }
}
