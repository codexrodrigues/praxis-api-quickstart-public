package com.example.praxis.apiquickstart.procurement.repository;

import com.example.praxis.apiquickstart.procurement.entity.ProcurementPurchaseOrder;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import java.util.List;

public interface ProcurementPurchaseOrderRepository extends BaseCrudRepository<ProcurementPurchaseOrder, Integer> {
    List<ProcurementPurchaseOrder> findBySupplierId(Integer supplierId);
    List<ProcurementPurchaseOrder> findByContractId(Integer contractId);
}
