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
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@ApiResource(
        value = ApiPaths.Procurement.COMPANIES,
        resourceKey = "procurement.companies",
        title = "Empresas Compradoras",
        description = "Empresas que delimitam escopo de fornecedores, contratos, produtos e pedidos na cadeia corporativa de compras.",
        icon = "business",
        visualTone = "procurement"
)
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

    @Override
    @PostMapping("/filter")
    @UiSurface(
            id = "buying-company-scope-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Escopo de empresas compradoras",
            description = "Mostra empresas por status, localidade e identidade fiscal para explicar de onde partem fornecedores, contratos, produtos e pedidos.",
            intent = "procurement-company-scope",
            order = 5,
            tags = {"procurement", "company", "supplier", "contract", "purchase-order"}
    )
    @Operation(
            summary = "Filtrar empresas compradoras",
            description = "Lista empresas que delimitam a cadeia de suprimentos por razao social, documento e status, servindo como raiz de fornecedores, contratos, produtos e pedidos."
    )
    public ResponseEntity<RestApiResponse<Page<EntityModel<ProcurementCompanyDTO>>>> filter(
            @RequestBody ProcurementCompanyFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "includeIds", required = false) List<Integer> includeIds,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }
}
