package com.example.praxis.apiquickstart.rulelab;

import java.util.Optional;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

public interface ExtraordinaryBenefitRequestRepository
        extends BaseCrudRepository<ExtraordinaryBenefitRequestEntity, Long> {
    Optional<ExtraordinaryBenefitRequestEntity> findByRequestReference(String requestReference);
}
