package com.example.praxis.apiquickstart.core.repository;

import com.example.praxis.apiquickstart.core.entity.ResourceActionExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ResourceActionExecutionRepository extends JpaRepository<ResourceActionExecution, UUID> {
    Optional<ResourceActionExecution> findByResourceKeyAndResourceIdAndActionIdAndActorSubjectAndIdempotencyKey(
            String resourceKey, String resourceId, String actionId, String actorSubject, String idempotencyKey);
}
