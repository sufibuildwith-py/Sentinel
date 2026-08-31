package com.sentinel.revenue.learning;
import com.sentinel.revenue.model.PolicyChangeProposal;
import com.sentinel.revenue.repository.PolicyChangeProposalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PolicyChangeWorkflowService {
    private final PolicyChangeProposalRepository proposals;
    public PolicyChangeWorkflowService(PolicyChangeProposalRepository proposals) { this.proposals=proposals; }
    @Transactional public PolicyChangeProposal propose(UUID modelId, String version, String proposal) {
        return proposals.saveAndFlush(new PolicyChangeProposal(modelId, version, proposal, Instant.now()));
    }
    @Transactional public PolicyChangeProposal recordReplay(UUID id, boolean passed) { PolicyChangeProposal p=get(id); p.recordReplay(passed); return proposals.saveAndFlush(p); }
    @Transactional public PolicyChangeProposal recordShadow(UUID id, boolean passed) { PolicyChangeProposal p=get(id); p.recordShadow(passed); return proposals.saveAndFlush(p); }
    @Transactional public PolicyChangeProposal approve(UUID id, String actor, String reason) { PolicyChangeProposal p=get(id); p.approve(actor,reason,Instant.now()); return proposals.saveAndFlush(p); }
    private PolicyChangeProposal get(UUID id){return proposals.findById(id).orElseThrow(() -> new IllegalArgumentException("Policy proposal not found: "+id));}
}
