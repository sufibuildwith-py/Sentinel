package com.sentinel.revenue.api;

import com.sentinel.revenue.portfolio.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue")
public class PortfolioController {
    private final RecoveryPortfolioOptimizer portfolios;
    private final HumanReviewOptimizer humanReviews;
    private final CustomerRecoveryProfileService profiles;

    public PortfolioController(RecoveryPortfolioOptimizer portfolios, HumanReviewOptimizer humanReviews,
                               CustomerRecoveryProfileService profiles) {
        this.portfolios = portfolios; this.humanReviews = humanReviews; this.profiles = profiles;
    }

    @PostMapping("/portfolio/optimize")
    public ResponseEntity<PortfolioAllocation> optimize(@RequestBody PortfolioOptimizationRequest request) {
        return ResponseEntity.ok(portfolios.optimize(request.candidates(), request.constraints()));
    }

    @PostMapping("/human-review/rank")
    public ResponseEntity<List<HumanReviewCandidate>> rankHumanReview(
            @RequestBody HumanReviewRankingRequest request) {
        return ResponseEntity.ok(humanReviews.rank(request.candidates(), request.capacity()));
    }

    @GetMapping("/incidents/{incidentId}/customer-recovery-profile")
    public ResponseEntity<CustomerRecoveryProfile> customerProfile(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(profiles.forIncident(incidentId));
    }

    public record PortfolioOptimizationRequest(List<PortfolioCandidate> candidates,
                                               PortfolioConstraints constraints) {
        public PortfolioOptimizationRequest { candidates = List.copyOf(candidates); }
    }
    public record HumanReviewRankingRequest(List<HumanReviewCandidate> candidates, int capacity) {
        public HumanReviewRankingRequest { candidates = List.copyOf(candidates); }
    }
}
