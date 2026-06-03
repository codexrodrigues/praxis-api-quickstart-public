package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementSupplierFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementSupplierMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementSupplierService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;

@ApiResource(value = ApiPaths.Procurement.SUPPLIERS, resourceKey = "procurement.suppliers")
@ApiGroup("procurement")
public class ProcurementSupplierController extends AbstractQuickstartCrudController<ProcurementSupplier, ProcurementSupplierDTO, Integer, ProcurementSupplierFilterDTO, CreateProcurementSupplierDTO, UpdateProcurementSupplierDTO> {
    private final ProcurementSupplierService service;
    private final ProcurementSupplierMapper mapper;

    public ProcurementSupplierController(ProcurementSupplierService service, ProcurementSupplierMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected ProcurementSupplierService getService() { return service; }

    @Override
    protected ProcurementSupplierDTO toDto(ProcurementSupplier entity) { return mapper.toDto(entity); }

    @Override
    protected ProcurementSupplier toEntity(ProcurementSupplierDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(ProcurementSupplier entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(ProcurementSupplierDTO dto) { return dto.getId(); }
}
