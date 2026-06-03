package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementProductDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementProductFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementProduct;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementProductMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementProductService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;

@ApiResource(value = ApiPaths.Procurement.PRODUCTS, resourceKey = "procurement.products")
@ApiGroup("procurement")
public class ProcurementProductController extends AbstractQuickstartCrudController<ProcurementProduct, ProcurementProductDTO, Integer, ProcurementProductFilterDTO, CreateProcurementProductDTO, UpdateProcurementProductDTO> {
    private final ProcurementProductService service;
    private final ProcurementProductMapper mapper;

    public ProcurementProductController(ProcurementProductService service, ProcurementProductMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected ProcurementProductService getService() { return service; }

    @Override
    protected ProcurementProductDTO toDto(ProcurementProduct entity) { return mapper.toDto(entity); }

    @Override
    protected ProcurementProduct toEntity(ProcurementProductDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(ProcurementProduct entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(ProcurementProductDTO dto) { return dto.getId(); }
}
