package com.sentinel.revenue.policy;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sentinel.revenue.economics.DecisionCertificateService;
import com.sentinel.revenue.governor.RecoverySafetyProperties;
import com.sentinel.revenue.model.CompiledPolicyConstitution;
import com.sentinel.revenue.repository.CompiledPolicyConstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class PolicyConstitutionCompiler {
    public static final String COMPILER_VERSION = "sentinel-policy-compiler-v1";
    private final PolicyProperties policy;
    private final RecoverySafetyProperties governor;
    private final CompiledPolicyConstitutionRepository constitutions;
    private final ObjectMapper canonicalJson;
    private final Clock clock;

    public PolicyConstitutionCompiler(PolicyProperties policy, RecoverySafetyProperties governor,
                                      CompiledPolicyConstitutionRepository constitutions,
                                      ObjectMapper objectMapper, Clock clock) {
        this.policy = policy; this.governor = governor; this.constitutions = constitutions;
        this.clock = clock; this.canonicalJson = objectMapper.copy().findAndRegisterModules()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public CompiledPreview preview(String merchantId, String policyVersion) {
        validateIdentity(merchantId, policyVersion);
        MerchantPolicyConstitution constitution = activeConstitution();
        return new CompiledPreview(merchantId, policyVersion, COMPILER_VERSION, constitution,
                hash(constitution), "COMPILED_PENDING_PROMOTION");
    }

    @Transactional
    public CompiledPolicyConstitution compile(String merchantId, String policyVersion,
                                              String benchmarkReference, String replayReference,
                                              String shadowReference) {
        CompiledPreview preview = preview(merchantId, policyVersion);
        return constitutions.findByMerchantIdAndPolicyVersion(merchantId, policyVersion)
                .orElseGet(() -> constitutions.saveAndFlush(new CompiledPolicyConstitution(UUID.randomUUID(),
                        merchantId, policyVersion, COMPILER_VERSION, preview.constitution(), preview.hash(),
                        null, null, benchmarkReference, replayReference, shadowReference,
                        "COMPILED_PENDING_PROMOTION", clock.instant())));
    }

    private MerchantPolicyConstitution activeConstitution() {
        return new MerchantPolicyConstitution(
                new MerchantPolicyConstitution.Authority("PROPOSE_ONLY", "DETERMINISTIC_ONLY"),
                new MerchantPolicyConstitution.Safety("BLOCK", "BLOCK", "BLOCK", "RECHECK"),
                new MerchantPolicyConstitution.Limits(policy.maximumAutoAmountMinor(), policy.maximumAttempts(),
                        governor.maxCustomerContacts(), governor.maxTotalValueMinor(), policy.actionTtl()),
                new MerchantPolicyConstitution.Escalation("HUMAN", "HUMAN", "BLOCK"),
                policy.allowedStrategies());
    }

    private String hash(MerchantPolicyConstitution constitution) {
        try { return DecisionCertificateService.hashText(canonicalJson.writeValueAsString(constitution)); }
        catch (Exception exception) { throw new IllegalStateException("Unable to hash compiled policy", exception); }
    }
    private void validateIdentity(String merchantId, String version) {
        if (merchantId == null || merchantId.isBlank() || version == null || version.isBlank())
            throw new IllegalArgumentException("merchantId and policyVersion are required");
    }

    public record CompiledPreview(String merchantId, String policyVersion, String compilerVersion,
                                  MerchantPolicyConstitution constitution, String hash, String status) { }
}
