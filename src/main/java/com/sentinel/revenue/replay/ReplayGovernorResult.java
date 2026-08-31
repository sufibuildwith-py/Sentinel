package com.sentinel.revenue.replay;
import java.util.List;
public record ReplayGovernorResult(boolean allowed,long allowedValueMinor,List<String> violations){public ReplayGovernorResult{violations=List.copyOf(violations);} public String disposition(){return allowed?"ALLOW":"DENY";}}
