package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.ShadowDecisionDifference; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ShadowDecisionDifferenceRepository extends JpaRepository<ShadowDecisionDifference,UUID>{List<ShadowDecisionDifference> findAllBySnapshotId(UUID snapshotId); boolean existsByCriticalRegressionTrue();}
