package com.sentinel.revenue.replay;

import com.sentinel.revenue.governor.*;
import com.sentinel.revenue.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ReplayGovernorEvaluator {
 private final RecoverySafetyProperties properties;
 public ReplayGovernorEvaluator(RecoverySafetyProperties properties){this.properties=properties;}
 public ReplayGovernorResult evaluate(GovernorReplayContext c,PolicyDecision policy){
  List<String> v=new ArrayList<>();
  if(policy==PolicyDecision.DENY)v.add("POLICY_DENY");
  if(policy==PolicyDecision.AUTO&&c.enabledKillSwitches().contains(KillSwitch.ALL_AUTONOMOUS_EXECUTION))v.add("KILL_SWITCH:ALL_AUTONOMOUS_EXECUTION");
  if(c.strategy()==RecoveryStrategy.ALTERNATIVE_PAYMENT_LINK&&c.enabledKillSwitches().contains(KillSwitch.PAYMENT_LINK_CREATION))v.add("KILL_SWITCH:PAYMENT_LINK_CREATION");
  if(c.requestedValueMinor()>properties.maxValuePerIncidentMinor())v.add("MAX_VALUE_PER_INCIDENT");
  if(c.activeTotalValueMinor()+c.requestedValueMinor()>properties.maxTotalValueMinor())v.add("MAX_TOTAL_VALUE");
  if(c.activeIncidents()>=properties.maxIncidents())v.add("MAX_INCIDENTS");
  if(c.providerCallsLastMinute()>=properties.maxProviderCallsPerMinute())v.add("MAX_PROVIDER_CALLS_PER_MINUTE");
  if(c.customerContacts()>=properties.maxCustomerContacts())v.add("MAX_CUSTOMER_CONTACTS");
  if(c.retryCount()>=properties.maxRetryCount())v.add("MAX_RETRY_COUNT");
  if(c.concurrentJobs()>=properties.maxConcurrentJobs())v.add("MAX_CONCURRENT_JOBS");
  if(c.toolFailureRate()>properties.maxToolFailureRate())v.add("MAX_TOOL_FAILURE_RATE");
  if(c.unreconciledValueMinor()+c.requestedValueMinor()>properties.maxUnreconciledValueMinor())v.add("MAX_UNRECONCILED_VALUE");
  return new ReplayGovernorResult(v.isEmpty(),v.isEmpty()?c.requestedValueMinor():0,v);
 }
}
