package com.example.praxis.apiquickstart.procurement.repository;

import com.example.praxis.apiquickstart.procurement.entity.ProcurementContract;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;

public interface ProcurementContractRepository extends BaseCrudRepository<ProcurementContract, Integer> {
    List<ProcurementContract> findBySupplierId(Integer supplierId);
}
