package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.PolicyReplaySnapshot; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface PolicyReplaySnapshotRepository extends JpaRepository<PolicyReplaySnapshot,UUID>{Optional<PolicyReplaySnapshot> findBySnapshotSha256(String hash);}
