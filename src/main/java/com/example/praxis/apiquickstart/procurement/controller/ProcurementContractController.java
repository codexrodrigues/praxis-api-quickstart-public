package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementContractFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementContract;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementContractMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementContractService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;

@ApiResource(
        value = ApiPaths.Procurement.CONTRACTS,
        resourceKey = "procurement.contracts",
        title = "Contratos",
        description = "Acordos comerciais, fornecedores, empresas, produtos e ordens de compra que governam suprimentos.",
        icon = "file-signature",
        visualTone = "procurement"
)
@ApiGroup("procurement")
public class ProcurementContractController extends AbstractQuickstartCrudController<ProcurementContract, ProcurementContractDTO, Integer, ProcurementContractFilterDTO, CreateProcurementContractDTO, UpdateProcurementContractDTO> {
    private final ProcurementContractService service;
    private final ProcurementContractMapper mapper;

    public ProcurementContractController(ProcurementContractService service, ProcurementContractMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected ProcurementContractService getService() { return service; }

    @Override
    protected ProcurementContractDTO toDto(ProcurementContract entity) { return mapper.toDto(entity); }

    @Override
    protected ProcurementContract toEntity(ProcurementContractDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(ProcurementContract entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(ProcurementContractDTO dto) { return dto.getId(); }
}
