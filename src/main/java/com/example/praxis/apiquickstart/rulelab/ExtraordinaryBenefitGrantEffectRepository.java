package com.example.praxis.apiquickstart.rulelab;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtraordinaryBenefitGrantEffectRepository
        extends JpaRepository<ExtraordinaryBenefitGrantEffect, Long> {
    Optional<ExtraordinaryBenefitGrantEffect> findByBenefitRequestId(Long benefitRequestId);
}
