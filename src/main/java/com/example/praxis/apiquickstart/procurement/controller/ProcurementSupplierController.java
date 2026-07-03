package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementSupplierDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementSupplierWorkflowRequestDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementSupplierWorkflowResultDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementSupplierFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementSupplier;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementSupplierMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.hateoas.Links;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
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
        return workflowResponse(id, "/{id}/actions/block", service.block(id, dto));
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
        return workflowResponse(id, "/{id}/actions/reinstate", service.reinstate(id, dto));
    }

    private ResponseEntity<RestApiResponse<ProcurementSupplierWorkflowResultDTO>> workflowResponse(
            Integer id,
            String operationPath,
            ProcurementSupplierWorkflowResultDTO result
    ) {
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema(operationPath, "post", "request"),
                linkToUiSchema(operationPath, "post", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(result, hateoasOrNull(links)));
    }
}
