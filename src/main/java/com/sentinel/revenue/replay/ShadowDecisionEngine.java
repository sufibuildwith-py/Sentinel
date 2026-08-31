package com.sentinel.revenue.replay;

import com.sentinel.revenue.model.*;
import com.sentinel.revenue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;

@Service
public class ShadowDecisionEngine {
 private final PolicyReplaySnapshotRepository snapshots; private final ShadowDecisionDifferenceRepository differences;
 private final PolicyReplayService replay; private final Clock clock;
 public ShadowDecisionEngine(PolicyReplaySnapshotRepository snapshots,ShadowDecisionDifferenceRepository differences,PolicyReplayService replay,Clock clock){this.snapshots=snapshots;this.differences=differences;this.replay=replay;this.clock=clock;}
 @Transactional public ShadowComparison compare(UUID snapshotId,ShadowCandidate candidate){
  PolicyReplaySnapshot s=snapshots.findById(snapshotId).orElseThrow(()->new IllegalArgumentException("Replay snapshot not found: "+snapshotId));
  PolicyReplayResult shadow=replay.replay(snapshotId,candidate.policyVersion());
  boolean actionChanged=!s.getProductionAction().equals(candidate.action());
  boolean policyChanged=s.getProductionPolicyResult()!=shadow.policyDecision();
  boolean governorChanged=!s.getProductionGovernorResult().equals(shadow.governorDisposition());
  boolean priorityChanged=candidate.priority()!=null;
  boolean rankingChanged=!candidate.opportunityRanking().isEmpty()&&!candidate.opportunityRanking().get(0).equals(candidate.action());
  boolean approvalChanged=requiresApproval(s.getProductionPolicyResult())!=requiresApproval(shadow.policyDecision());
  boolean critical=(s.getProductionPolicyResult()==PolicyDecision.DENY&&shadow.policyDecision()!=PolicyDecision.DENY)
    ||("DENY".equals(s.getProductionGovernorResult())&&"ALLOW".equals(shadow.governorDisposition()));
  String explanation="SHADOW ONLY; no execution authority. "+candidate.explanation();
  ShadowDecisionDifference saved=differences.saveAndFlush(new ShadowDecisionDifference(snapshotId,s.getProductionAction(),candidate.action(),s.getProductionPolicyResult(),shadow.policyDecision(),s.getProductionGovernorResult(),shadow.governorDisposition(),s.getProductionPredictedValueMinor(),candidate.predictedValueMinor(),s.getProductionConfidence(),candidate.confidence(),null,candidate.priority(),rankingChanged,approvalChanged,explanation,critical,clock.instant()));
  return new ShadowComparison(saved.getId(),snapshotId,s.getProductionAction(),candidate.action(),s.getProductionPolicyResult(),shadow.policyDecision(),s.getProductionGovernorResult(),shadow.governorDisposition(),s.getProductionPredictedValueMinor(),candidate.predictedValueMinor(),s.getProductionConfidence(),candidate.confidence(),actionChanged,policyChanged,governorChanged,priorityChanged,rankingChanged,approvalChanged,critical,explanation,s.getFeatureSchemaVersion(),candidate.modelVersion(),shadow.policyVersion(),s.getStrategyVersion(),s.getGovernorVersion());
 }
 @Transactional(readOnly=true) public List<ShadowDecisionDifference> differences(UUID snapshotId){return differences.findAllBySnapshotId(snapshotId);}
 private boolean requiresApproval(PolicyDecision d){return d==PolicyDecision.HUMAN;}
}
