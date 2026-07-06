package com.example.praxis.apiquickstart.procurement.repository;

import com.example.praxis.apiquickstart.procurement.entity.ProcurementProduct;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;

public interface ProcurementProductRepository extends BaseCrudRepository<ProcurementProduct, Integer> {
    List<ProcurementProduct> findByContractId(Integer contractId);
}
