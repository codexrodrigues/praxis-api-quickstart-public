package com.example.praxis.apiquickstart.procurement.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.procurement.dto.CreateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.ProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.UpdateProcurementContractDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementContractWorkflowRequestDTO;
import com.example.praxis.apiquickstart.procurement.dto.actions.ProcurementContractWorkflowResultDTO;
import com.example.praxis.apiquickstart.procurement.dto.filter.ProcurementContractFilterDTO;
import com.example.praxis.apiquickstart.procurement.entity.ProcurementContract;
import com.example.praxis.apiquickstart.procurement.mapper.ProcurementContractMapper;
import com.example.praxis.apiquickstart.procurement.service.ProcurementContractService;
import io.swagger.v3.oas.annotations.Operation;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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

    @Override
    @PostMapping("/filter")
    @UiSurface(
            id = "contract-governance-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Governanca de contratos",
            description = "Organiza contratos por empresa, fornecedor, vigencia e status para orientar compras, bloqueios e auditoria de fornecimento.",
            intent = "procurement-contract-governance",
            order = 20,
            tags = {"procurement", "contract", "supplier", "compliance"}
    )
    @Operation(
            summary = "Filtrar contratos de fornecimento",
            description = "Lista contratos por empresa, fornecedor, vigencia, moeda e status para governar pedidos de compra e compliance de suprimentos."
    )
    public ResponseEntity<RestApiResponse<Page<EntityModel<ProcurementContractDTO>>>> filter(
            @RequestBody ProcurementContractFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "includeIds", required = false) List<Integer> includeIds,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/{id}/actions/sign")
    @Operation(
            summary = "Assinar contrato de fornecimento",
            description = "Promove um contrato em rascunho ou ativo para assinado, tornando-o elegivel para pedidos governados quando fornecedor e produto tambem estiverem liberados."
    )
    @WorkflowAction(
            id = "sign",
            title = "Assinar contrato",
            description = "Confirma o contrato como fonte governada para compras e libera sua selecao em pedidos.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Contrato assinado",
            allowedStates = {"DRAFT", "ACTIVE"},
            tags = {"workflow", "procurement", "contract", "purchase-order"}
    )
    public ResponseEntity<RestApiResponse<ProcurementContractWorkflowResultDTO>> sign(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ProcurementContractWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/sign", service.sign(id, dto));
    }

    @PostMapping("/{id}/actions/suspend")
    @Operation(
            summary = "Suspender contrato de fornecimento",
            description = "Suspende temporariamente um contrato assinado ou ativo, registrando motivo e removendo sua elegibilidade em lookups de pedidos."
    )
    @WorkflowAction(
            id = "suspend",
            title = "Suspender contrato",
            description = "Bloqueia o contrato para novos pedidos por revisao comercial, risco, compliance ou expiracao antecipada.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Contrato suspenso",
            allowedStates = {"SIGNED", "ACTIVE"},
            tags = {"workflow", "procurement", "contract", "compliance"}
    )
    public ResponseEntity<RestApiResponse<ProcurementContractWorkflowResultDTO>> suspend(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ProcurementContractWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/suspend", service.suspend(id, dto));
    }

    @PostMapping("/{id}/actions/reactivate")
    @Operation(
            summary = "Reativar contrato suspenso",
            description = "Reativa um contrato suspenso apos revisao de negocio, limpando o motivo de bloqueio e devolvendo elegibilidade operacional."
    )
    @WorkflowAction(
            id = "reactivate",
            title = "Reativar contrato",
            description = "Restaura a elegibilidade do contrato quando a revisao libera novas compras.",
            scope = ActionScope.ITEM,
            order = 120,
            successMessage = "Contrato reativado",
            allowedStates = {"SUSPENDED"},
            tags = {"workflow", "procurement", "contract", "compliance"}
    )
    public ResponseEntity<RestApiResponse<ProcurementContractWorkflowResultDTO>> reactivate(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ProcurementContractWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/reactivate", service.reactivate(id, dto));
    }

    private ResponseEntity<RestApiResponse<ProcurementContractWorkflowResultDTO>> workflowResponse(
            Integer id,
            String operationPath,
            ProcurementContractWorkflowResultDTO result
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
