package com.sentinel.revenue.api;

import com.sentinel.revenue.governor.DynamicGovernorAssessment;
import com.sentinel.revenue.governor.DynamicRecoveryGovernor;
import com.sentinel.revenue.governor.GovernorSignalSnapshot;
import com.sentinel.revenue.governor.RecoverySafetyProperties;
import com.sentinel.revenue.model.CompiledPolicyConstitution;
import com.sentinel.revenue.policy.PolicyConstitutionCompiler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/revenue/governance")
public class PolicyConstitutionController {
    private final PolicyConstitutionCompiler compiler;
    private final DynamicRecoveryGovernor governor;
    private final RecoverySafetyProperties safetyProperties;

    public PolicyConstitutionController(PolicyConstitutionCompiler compiler,
                                        DynamicRecoveryGovernor governor,
                                        RecoverySafetyProperties safetyProperties) {
        this.compiler = compiler; this.governor = governor; this.safetyProperties = safetyProperties;
    }

    @GetMapping("/constitution/preview")
    public ResponseEntity<PolicyConstitutionCompiler.CompiledPreview> preview(
            @RequestParam String merchantId, @RequestParam String policyVersion) {
        return ResponseEntity.ok(compiler.preview(merchantId, policyVersion));
    }

    @PostMapping("/constitution/compile")
    public ResponseEntity<CompiledPolicyConstitution> compile(@RequestBody CompileRequest request) {
        return ResponseEntity.ok(compiler.compile(request.merchantId(), request.policyVersion(),
                request.benchmarkReference(), request.replayReference(), request.shadowReference()));
    }

    @PostMapping("/governor/assess")
    public ResponseEntity<DynamicGovernorAssessment> assess(@RequestBody GovernorSignalSnapshot snapshot) {
        return ResponseEntity.ok(governor.assess(snapshot, safetyProperties.maxToolFailureRate()));
    }

    public record CompileRequest(String merchantId, String policyVersion, String benchmarkReference,
                                 String replayReference, String shadowReference) { }
}
