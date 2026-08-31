package com.sentinel.revenue.repository;
import com.sentinel.revenue.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RegisteredModelRepository extends JpaRepository<RegisteredModel, UUID> {
    Optional<RegisteredModel> findByModelNameAndModelVersion(String modelName, String modelVersion);
    List<RegisteredModel> findAllByLifecycle(ModelLifecycle lifecycle);
}
