package com.sentinel.revenue.economics;

import com.sentinel.revenue.model.RecoveryCostEntry;
import com.sentinel.revenue.repository.RecoveryCostEntryRepository;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RecoveryCostLedgerService {
    private final RecoveryCostEntryRepository costs;
    private final RevenueIncidentRepository incidents;
    private final Clock clock;

    public RecoveryCostLedgerService(RecoveryCostEntryRepository costs,
                                     RevenueIncidentRepository incidents, Clock clock) {
        this.costs = costs;
        this.incidents = incidents;
        this.clock = clock;
    }

    @Transactional
    public RecoveryCostEntry append(RecoveryCostCommand command) {
        validate(command);
        if (!incidents.existsById(command.incidentId())) {
            throw new IllegalArgumentException("Revenue incident not found: " + command.incidentId());
        }
        Instant now = clock.instant();
        return costs.saveAndFlush(new RecoveryCostEntry(command.incidentId(), command.recoveryActionId(),
                command.decisionId(), command.category(), command.amountMinor(),
                command.currency().toUpperCase(Locale.ROOT), command.source(), command.calculationMethod(),
                command.evidenceQuality(), command.version(),
                command.occurredAt() == null ? now : command.occurredAt(), now));
    }

    @Transactional(readOnly = true)
    public List<RecoveryCostEntry> forIncident(UUID incidentId) {
        return costs.findAllByIncidentIdOrderByOccurredAtAsc(incidentId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRecordedMinor() {
        return costs.findAll().stream().map(RecoveryCostEntry::getAmountMinor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validate(RecoveryCostCommand command) {
        if (command == null || command.incidentId() == null || command.category() == null
                || command.amountMinor() == null || command.amountMinor().signum() < 0
                || command.currency() == null || !command.currency().matches("[A-Za-z]{3}")
                || blank(command.source()) || blank(command.calculationMethod())
                || command.evidenceQuality() == null || blank(command.version())) {
            throw new IllegalArgumentException("A complete, non-negative and attributed recovery cost is required");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
