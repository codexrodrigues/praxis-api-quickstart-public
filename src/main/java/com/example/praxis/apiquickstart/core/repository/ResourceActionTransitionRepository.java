package com.example.praxis.apiquickstart.core.repository;

import com.example.praxis.apiquickstart.core.entity.ResourceActionTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface ResourceActionTransitionRepository extends JpaRepository<ResourceActionTransition, UUID> {
    Optional<ResourceActionTransition> findByResourceKeyAndResourceIdAndActionIdAndIdempotencyKey(
            String resourceKey, String resourceId, String actionId, String idempotencyKey);
}
