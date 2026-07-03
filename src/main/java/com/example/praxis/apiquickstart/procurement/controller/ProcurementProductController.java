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
        value = ApiPaths.Procurement.PRODUCTS,
        resourceKey = "procurement.products",
        title = "Produtos Contratados",
        description = "Produtos e servicos disponiveis para compra, governados por empresa, contrato, estoque, unidade, categoria e elegibilidade.",
        icon = "inventory-2",
        visualTone = "procurement"
)
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

    @Override
    @PostMapping("/filter")
    @UiSurface(
            id = "contract-product-catalog-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Catalogo de produtos contratados",
            description = "Mostra produtos por empresa, contrato, categoria, unidade, estoque e status para explicar o que pode ser comprado em cada acordo.",
            intent = "procurement-contract-product-catalog",
            order = 25,
            tags = {"procurement", "product", "contract", "catalog", "stock"}
    )
    @Operation(
            summary = "Filtrar produtos contratados",
            description = "Lista produtos e servicos por empresa, contrato, nome e status para materializar catalogos governados e pedidos de compra elegiveis."
    )
    public ResponseEntity<RestApiResponse<Page<EntityModel<ProcurementProductDTO>>>> filter(
            @RequestBody ProcurementProductFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "includeIds", required = false) List<Integer> includeIds,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }
}
