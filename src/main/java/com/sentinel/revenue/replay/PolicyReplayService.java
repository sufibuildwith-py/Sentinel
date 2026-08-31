package com.sentinel.revenue.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.revenue.model.*;
import com.sentinel.revenue.policy.*;
import com.sentinel.revenue.repository.PolicyReplaySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PolicyReplayService {
 private final PolicyReplaySnapshotRepository snapshots; private final PolicyEngine activePolicy;
 private final ReplayGovernorEvaluator governor; private final ObjectMapper json; private final Clock clock;
 public PolicyReplayService(PolicyReplaySnapshotRepository snapshots,PolicyEngine activePolicy,ReplayGovernorEvaluator governor,ObjectMapper json,Clock clock){this.snapshots=snapshots;this.activePolicy=activePolicy;this.governor=governor;this.json=json;this.clock=clock;}
 @Transactional public PolicyReplaySnapshot capture(ReplayCaptureCommand c){
  String hash=hash(c); return snapshots.findBySnapshotSha256(hash).orElseGet(()->snapshots.saveAndFlush(new PolicyReplaySnapshot(c.incidentId(),c.policyContext(),c.governorContext(),c.featureSchemaVersion(),c.modelVersion(),c.policyVersion(),c.strategyVersion(),c.governorVersion(),c.seed(),c.productionPolicyResult(),c.productionAction(),c.productionGovernorResult(),c.productionPredictedValueMinor(),c.productionConfidence(),hash,clock.instant())));
 }
 @Transactional(readOnly=true) public PolicyReplayResult replay(UUID snapshotId){return replay(snapshotId,null);}
 @Transactional(readOnly=true) public PolicyReplayResult replay(UUID snapshotId,ReplayPolicyVersion candidate){
  PolicyReplaySnapshot s=snapshots.findById(snapshotId).orElseThrow(()->new IllegalArgumentException("Replay snapshot not found: "+snapshotId));
  PolicyEngine engine=candidate==null?activePolicy:new PolicyEngine(candidate.properties(),new MandatoryStopEvaluator(candidate.properties()));
  PolicyEvaluation p=engine.evaluate(s.getPolicyContext()); ReplayGovernorResult g=governor.evaluate(s.getGovernorContext(),p.decision());
  return new PolicyReplayResult(s.getId(),s.getSnapshotSha256(),s.getReplaySeed(),s.getFeatureSchemaVersion(),s.getModelVersion(),candidate==null?s.getPolicyVersion():candidate.version(),s.getStrategyVersion(),s.getGovernorVersion(),p.decision(),g.disposition(),g.allowedValueMinor(),p.rules(),g.violations(),s.getCapturedAt().toString());
 }
 private String hash(ReplayCaptureCommand c){try{byte[] canonical=json.writeValueAsString(c).getBytes(StandardCharsets.UTF_8);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));}catch(Exception e){throw new IllegalStateException("Replay snapshot hashing failed",e);}}
}
