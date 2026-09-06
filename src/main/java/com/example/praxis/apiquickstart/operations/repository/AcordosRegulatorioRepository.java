package com.example.praxis.apiquickstart.operations.repository;

import com.example.praxis.apiquickstart.operations.entity.AcordosRegulatorio;
import com.example.praxis.apiquickstart.operations.enums.AcordoStatus;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AcordosRegulatorioRepository extends BaseCrudRepository<AcordosRegulatorio, Integer> {
    @Query("select a.status from AcordosRegulatorio a where a.id = :id")
    Optional<AcordoStatus> findStatusById(@Param("id") Integer id);
}


