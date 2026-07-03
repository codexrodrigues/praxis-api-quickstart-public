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
        value = ApiPaths.Procurement.PURCHASE_ORDERS,
        resourceKey = "procurement.purchase-orders",
        title = "Pedidos de Compra",
        description = "Pedidos que materializam compra governada por empresa, fornecedor homologado, contrato, produto, quantidade e moeda.",
        icon = "shopping-cart-checkout",
        visualTone = "procurement"
)
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

    @Override
    @PostMapping("/filter")
    @UiSurface(
            id = "purchase-order-control-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Controle de pedidos de compra",
            description = "Acompanha pedidos por empresa, fornecedor, contrato, produto, data e moeda para evidenciar a jornada de suprimentos no cockpit.",
            intent = "procurement-purchase-order-control",
            order = 30,
            tags = {"procurement", "purchase-order", "supplier", "contract"}
    )
    @Operation(
            summary = "Filtrar pedidos de compra",
            description = "Lista pedidos de compra com filtros por empresa, fornecedor, contrato, produto e periodo para materializar tabelas, dashboards e investigacao de elegibilidade."
    )
    public ResponseEntity<RestApiResponse<Page<EntityModel<ProcurementPurchaseOrderDTO>>>> filter(
            @RequestBody ProcurementPurchaseOrderFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "includeIds", required = false) List<Integer> includeIds,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @Override
    @PostMapping
    @UiSurface(
            id = "issue-purchase-order",
            kind = SurfaceKind.FORM,
            scope = SurfaceScope.COLLECTION,
            title = "Emitir pedido governado",
            description = "Formulario de emissao que encadeia empresa, fornecedor elegivel, contrato e produto antes da validacao backend de suprimentos.",
            intent = "procurement-purchase-order-issue",
            order = 40,
            tags = {"procurement", "purchase-order", "form", "governed-lookup"}
    )
    @Operation(
            summary = "Cadastrar pedido de compra",
            description = "Cria um pedido de compra e valida fornecedor selecionado contra a politica backend publicada por regras governadas."
    )
    public ResponseEntity<RestApiResponse<ProcurementPurchaseOrderDTO>> create(
            @jakarta.validation.Valid @RequestBody CreateProcurementPurchaseOrderDTO dto
    ) {
        return super.create(dto);
    }
}
