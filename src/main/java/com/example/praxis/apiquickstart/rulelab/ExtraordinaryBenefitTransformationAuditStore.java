package com.example.praxis.apiquickstart.rulelab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/** Insert-only persistence boundary for immutable transformation audit evidence. */
@Repository
class ExtraordinaryBenefitTransformationAuditStore {
    @PersistenceContext(unitName = "api")
    private EntityManager entityManager;

    void append(ExtraordinaryBenefitTransformationAudit audit) {
        entityManager.persist(audit);
    }
}
