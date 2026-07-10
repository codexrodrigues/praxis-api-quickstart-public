package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementPurchaseOrderDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementSupplierWorkflowRequestDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementSupplierWorkflowResultDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementSupplierFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementSupplierMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementContractService;
import com.example.praxis.apiquickstart.procurement.service.ProcurementPurchaseOrderService;
import com.example.praxis.apiquickstart.procurement.service.ProcurementSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.command.ResourceCommandExecutionResult;
import org.praxisplatform.uischema.command.ResourceCommandResponsePolicy;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.RelatedResourceChildOperation;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.hateoas.Links;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@ApiResource(
        value = ApiPaths.Procurement.SUPPLIERS,
        resourceKey = "procurement.suppliers",
        title = "Fornecedores",
        description = "Fornecedores homologaveis, elegibilidade de compra, risco de terceiro e bloqueios de compliance.",
        icon = "storefront",
        visualTone = "procurement"
)
@ApiGroup("procurement")
public class ProcurementSupplierController extends AbstractQuickstartCrudController<ProcurementSupplier, ProcurementSupplierDTO, Integer, ProcurementSupplierFilterDTO, CreateProcurementSupplierDTO, UpdateProcurementSupplierDTO> {
    private final ProcurementSupplierService service;
    private final ProcurementSupplierMapper mapper;
    private final ProcurementContractService contractService;
    private final ProcurementPurchaseOrderService purchaseOrderService;

    public ProcurementSupplierController(
            ProcurementSupplierService service,
            ProcurementSupplierMapper mapper,
            ProcurementContractService contractService,
            ProcurementPurchaseOrderService purchaseOrderService) {
        this.service = service;
        this.mapper = mapper;
        this.contractService = contractService;
        this.purchaseOrderService = purchaseOrderService;
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

    @Override
    @PostMapping("/filter")
    @UiSurface(
            id = "supplier-homologation-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Homologacao de fornecedores",
            description = "Lista fornecedores por empresa, risco, status e homologacao para apoiar compra governada, KYC e bloqueios de compliance.",
            intent = "procurement-supplier-governance",
            order = 10,
            tags = {"procurement", "supplier", "compliance", "lookup"}
    )
    @Operation(
            summary = "Filtrar fornecedores governados",
            description = "Lista fornecedores por empresa, documento, homologacao, risco e status para selecao elegivel em pedidos de compra e auditoria de terceiros."
    )
    public ResponseEntity<RestApiResponse<Page<EntityModel<ProcurementSupplierDTO>>>> filter(
            @RequestBody ProcurementSupplierFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "includeIds", required = false) List<Integer> includeIds,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @GetMapping("/{id}/contracts")
    @UiSurface(
            id = "supplier-contracts",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Contratos do fornecedor",
            description = "Lista contratos vinculados ao fornecedor para visualizar vigencia, status, moeda e elegibilidade antes de emitir pedidos.",
            intent = "procurement-supplier-contracts",
            order = 30,
            tags = {"procurement", "supplier", "contract", "read-projection", "related-resource"},
            relatedChildResourceKey = "procurement.contracts",
            relatedChildResourcePath = ApiPaths.Procurement.CONTRACTS,
            relatedChildParentField = "supplierId",
            relatedSelectable = true,
            relatedSelectionKeyField = "id",
            relatedChildOperations = {
                    RelatedResourceChildOperation.FILTER,
                    RelatedResourceChildOperation.LIST,
                    RelatedResourceChildOperation.CREATE,
                    RelatedResourceChildOperation.UPDATE,
                    RelatedResourceChildOperation.DELETE
            }
    )
    @ResourceIntent(
            id = "procurement-supplier-contracts",
            title = "Contratos governados do fornecedor",
            description = "Mostra quais contratos tornam o fornecedor elegivel ou restrito para compras governadas.",
            order = 30
    )
    @Operation(summary = "Obter contratos do fornecedor", description = "Retorna contratos associados ao fornecedor selecionado para navegação contextual no cockpit.")
    public ResponseEntity<RestApiResponse<List<ProcurementContractDTO>>> getSupplierContracts(@PathVariable Integer id) {
        List<ProcurementContractDTO> contracts = contractService.findBySupplierIdForSupplierSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/contracts", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(contracts, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/purchase-orders")
    @UiSurface(
            id = "supplier-purchase-orders",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Pedidos do fornecedor",
            description = "Lista pedidos de compra emitidos para o fornecedor para acompanhar aprovacao, recebimento, cancelamento e recorrencia operacional.",
            intent = "procurement-supplier-purchase-orders",
            order = 40,
            tags = {"procurement", "supplier", "purchase-order", "workflow", "read-projection", "related-resource"},
            relatedChildResourceKey = "procurement.purchase-orders",
            relatedChildResourcePath = ApiPaths.Procurement.PURCHASE_ORDERS,
            relatedChildParentField = "supplierId",
            relatedSelectable = true,
            relatedSelectionKeyField = "id",
            relatedChildOperations = {
                    RelatedResourceChildOperation.FILTER,
                    RelatedResourceChildOperation.LIST,
                    RelatedResourceChildOperation.CREATE,
                    RelatedResourceChildOperation.UPDATE,
                    RelatedResourceChildOperation.DELETE
            }
    )
    @ResourceIntent(
            id = "procurement-supplier-purchase-orders",
            title = "Pedidos governados do fornecedor",
            description = "Mostra a execucao operacional do fornecedor em pedidos aprovados, recebidos ou cancelados.",
            order = 40
    )
    @Operation(summary = "Obter pedidos do fornecedor", description = "Retorna pedidos de compra associados ao fornecedor selecionado para navegação contextual no cockpit.")
    public ResponseEntity<RestApiResponse<List<ProcurementPurchaseOrderDTO>>> getSupplierPurchaseOrders(@PathVariable Integer id) {
        List<ProcurementPurchaseOrderDTO> purchaseOrders = purchaseOrderService.findBySupplierIdForSupplierSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/purchase-orders", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(purchaseOrders, hateoasOrNull(links)));
    }

    @PostMapping("/{id}/actions/block")
    @Operation(
            summary = "Bloquear fornecedor",
            description = "Bloqueia um fornecedor para novos pedidos, preservando motivo de compliance e refletindo a inelegibilidade em option sources governadas."
    )
    @WorkflowAction(
            id = "block",
            title = "Bloquear fornecedor",
            description = "Remove temporariamente a elegibilidade de compra do fornecedor por risco, compliance ou auditoria.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Fornecedor bloqueado",
            allowedStates = {"ACTIVE", "APPROVED"},
            tags = {"workflow", "procurement", "supplier", "compliance"}
    )
    public ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>> block(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ProcurementSupplierWorkflowRequestDTO dto
    ) {
        return governedBlock(id, dto);
    }

    @PostMapping("/{id}/actions/reinstate")
    @Operation(
            summary = "Reabilitar fornecedor",
            description = "Reabilita um fornecedor bloqueado ou inativo para compras, limpando o motivo de bloqueio e republicando elegibilidade operacional."
    )
    @WorkflowAction(
            id = "reinstate",
            title = "Reabilitar fornecedor",
            description = "Restaura a elegibilidade de compra quando a revisao de compliance libera o fornecedor.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Fornecedor reabilitado",
            allowedStates = {"BLOCKED", "INACTIVE"},
            tags = {"workflow", "procurement", "supplier", "compliance"}
    )
    public ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>> reinstate(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ProcurementSupplierWorkflowRequestDTO dto
    ) {
        return governedReinstate(id, dto);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>> governedBlock(
            Integer id,
            ProcurementSupplierWorkflowRequestDTO dto
    ) {
        return (ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>>) (ResponseEntity<?>) executeItemCommand(
                "block",
                id,
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> ResourceCommandExecutionResult.success(
                        request,
                        id,
                        service.block(id, dto),
                        java.util.Map.of("resourceKey", "procurement.suppliers")
                )
        );
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>> governedReinstate(
            Integer id,
            ProcurementSupplierWorkflowRequestDTO dto
    ) {
        return (ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>>) (ResponseEntity<?>) executeItemCommand(
                "reinstate",
                id,
                dto,
                ResourceCommandResponsePolicy.RETURN_COMMAND_RESULT,
                request -> ResourceCommandExecutionResult.success(
                        request,
                        id,
                        service.reinstate(id, dto),
                        java.util.Map.of("resourceKey", "procurement.suppliers")
                )
        );
    }
}
