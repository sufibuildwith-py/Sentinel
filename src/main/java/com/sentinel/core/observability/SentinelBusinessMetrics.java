package com.sentinel.core.observability;

import com.sentinel.revenue.execution.RecoveryExecutionResponse;
import com.sentinel.revenue.model.PolicyDecision;
import com.sentinel.revenue.model.RecoveryActionStatus;
import com.sentinel.revenue.planning.RecoveryPlanningResult;
import com.sentinel.revenue.repository.RevenueIncidentRepository;
import com.sentinel.revenue.webhook.WebhookResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public final class SentinelBusinessMetrics {
    private final MeterRegistry meters;
    private final RevenueIncidentRepository incidents;

    public SentinelBusinessMetrics(MeterRegistry meters, RevenueIncidentRepository incidents) {
        this.meters = meters;
        this.incidents = incidents;
    }

    @Around("execution(* com.sentinel.revenue.service.FailureClusteringService.onEventsPersisted(..))")
    public Object recordCreatedIncidents(ProceedingJoinPoint invocation) throws Throwable {
        long before = incidents.count();
        Object result = invocation.proceed();
        long created = Math.max(0, incidents.count() - before);
        if (created > 0) {
            meters.counter("sentinel.incidents.created").increment(created);
        }
        return result;
    }

    @Around("execution(* com.sentinel.revenue.service.RevenueInvestigationService.investigate(..))")
    public Object timeAgentInvestigation(ProceedingJoinPoint invocation) throws Throwable {
        Timer.Sample sample = Timer.start(meters);
        try {
            return invocation.proceed();
        } finally {
            sample.stop(meters.timer("sentinel.agent.duration"));
        }
    }

    @Around("execution(* com.sentinel.revenue.planning.RecoveryPlanningService.plan(..))")
    public Object recordPolicyDecision(ProceedingJoinPoint invocation) throws Throwable {
        Object result = invocation.proceed();
        if (result instanceof RecoveryPlanningResult planning) {
            PolicyDecision decision = planning.policyDecision();
            switch (decision) {
                case AUTO -> meters.counter("sentinel.policy.approved").increment();
                case HUMAN -> meters.counter("sentinel.policy.human_required").increment();
                case DENY -> meters.counter("sentinel.policy.blocked").increment();
            }
        }
        return result;
    }

    @Around("execution(* com.sentinel.revenue.execution.RecoveryExecutionService.execute(..))")
    public Object recordRecoveryExecution(ProceedingJoinPoint invocation) throws Throwable {
        meters.counter("sentinel.executions.attempted").increment();
        Timer.Sample sample = Timer.start(meters);
        try {
            Object result = invocation.proceed();
            if (result instanceof RecoveryExecutionResponse execution
                    && execution.actionStatus() == RecoveryActionStatus.EXECUTED) {
                meters.counter("sentinel.executions.succeeded").increment();
            } else {
                meters.counter("sentinel.executions.failed").increment();
            }
            return result;
        } catch (Throwable failure) {
            meters.counter("sentinel.executions.failed").increment();
            throw failure;
        } finally {
            sample.stop(meters.timer("sentinel.recovery.duration"));
        }
    }

    @Around("execution(* com.sentinel.revenue.webhook.WebhookRequestHandler.handle(..))")
    public Object recordWebhook(ProceedingJoinPoint invocation) throws Throwable {
        meters.counter("sentinel.webhooks.received").increment();
        Object result = invocation.proceed();
        if (result instanceof WebhookResult webhook && webhook.duplicate()) {
            meters.counter("sentinel.webhooks.duplicate").increment();
        }
        return result;
    }
}
