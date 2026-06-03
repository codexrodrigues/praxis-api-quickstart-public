package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementPurchaseOrderFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementPurchaseOrder;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementPurchaseOrderMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementPurchaseOrderService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;

@ApiResource(value = ApiPaths.Procurement.PURCHASE_ORDERS, resourceKey = "procurement.purchase-orders")
@ApiGroup("procurement")
public class ProcurementPurchaseOrderController extends AbstractQuickstartCrudController<ProcurementPurchaseOrder, ProcurementPurchaseOrderDTO, Integer, ProcurementPurchaseOrderFilterDTO, CreateProcurementPurchaseOrderDTO, UpdateProcurementPurchaseOrderDTO> {
    private final ProcurementPurchaseOrderService service;
    private final ProcurementPurchaseOrderMapper mapper;

    public ProcurementPurchaseOrderController(ProcurementPurchaseOrderService service, ProcurementPurchaseOrderMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected ProcurementPurchaseOrderService getService() { return service; }

    @Override
    protected ProcurementPurchaseOrderDTO toDto(ProcurementPurchaseOrder entity) { return mapper.toDto(entity); }

    @Override
    protected ProcurementPurchaseOrder toEntity(ProcurementPurchaseOrderDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(ProcurementPurchaseOrder entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(ProcurementPurchaseOrderDTO dto) { return dto.getId(); }
}
