package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementCompanyDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementCompanyFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementCompany;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementCompanyMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementCompanyService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;

@ApiResource(value = ApiPaths.Procurement.COMPANIES, resourceKey = "procurement.companies")
@ApiGroup("procurement")
public class ProcurementCompanyController extends AbstractQuickstartCrudController<ProcurementCompany, ProcurementCompanyDTO, Integer, ProcurementCompanyFilterDTO, CreateProcurementCompanyDTO, UpdateProcurementCompanyDTO> {
    private final ProcurementCompanyService service;
    private final ProcurementCompanyMapper mapper;

    public ProcurementCompanyController(ProcurementCompanyService service, ProcurementCompanyMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected ProcurementCompanyService getService() { return service; }

    @Override
    protected ProcurementCompanyDTO toDto(ProcurementCompany entity) { return mapper.toDto(entity); }

    @Override
    protected ProcurementCompany toEntity(ProcurementCompanyDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(ProcurementCompany entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(ProcurementCompanyDTO dto) { return dto.getId(); }
}
