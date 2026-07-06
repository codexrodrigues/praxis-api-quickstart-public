package com.example.praxis.apiquickstart.operationalassets.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowResultDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Equipamento;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoMapper;
import com.example.praxis.apiquickstart.operationalassets.service.EquipamentoAlocacaoService;
import com.example.praxis.apiquickstart.operationalassets.service.EquipamentoService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.praxisplatform.uischema.action.ActionScope;
import org.praxisplatform.uischema.surface.RelatedResourceChildOperation;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@ApiResource(
        value = ApiPaths.Assets.EQUIPAMENTOS,
        resourceKey = "assets.equipamentos",
        title = "Equipamentos",
        description = "Inventario de equipamentos operacionais, disponibilidade, resistencia, custodiante e elegibilidade para alocacao.",
        icon = "package",
        visualTone = "assets"
)
@ApiGroup("assets")
/**
 * Controller de referência para inventário de equipamentos.
 *
 * <p>Este recurso mostra como o quickstart apresenta ativos patrimoniais no
 * mesmo modelo metadata-driven usado pelos demais domínios da plataforma.
 * Como exemplo público, ele ajuda a orientar filtros de inventário, lookup
 * incremental e reuso do cadastro de ativos em missões, alocações e fluxos
 * operacionais sem contratos HTTP ad hoc.</p>
 */
public class EquipamentoController extends AbstractQuickstartCrudController<Equipamento, EquipamentoDTO, Integer, EquipamentoFilterDTO, CreateEquipamentoDTO, UpdateEquipamentoDTO> {

    private final EquipamentoService service;
    private final EquipamentoMapper mapper;
    private final EquipamentoAlocacaoService alocacaoService;

    @Autowired
    public EquipamentoController(
            EquipamentoService service,
            EquipamentoMapper mapper,
            EquipamentoAlocacaoService alocacaoService) {
        this.service = service;
        this.mapper = mapper;
        this.alocacaoService = alocacaoService;
    }

    @Override
    protected EquipamentoService getService() { return service; }

    @Override
    protected EquipamentoDTO toDto(Equipamento entity) { return mapper.toDto(entity); }

    @Override
    protected Equipamento toEntity(EquipamentoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Equipamento entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(EquipamentoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "equipment-inventory-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Inventario de equipamentos",
            description = "Mostra equipamentos por tipo, custodiante, resistencia e status para planejar alocacoes e auditoria patrimonial.",
            intent = "assets-equipment-inventory",
            order = 10,
            tags = {"assets", "equipment", "inventory", "lookup"}
    )
    @Operation(summary = "Filtrar equipamentos operacionais", description = "Lista equipamentos por tipo, capacidade, proprietário, disponibilidade e status de uso para inventário operacional e preparo de campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<EquipamentoDTO>>>> filter(@RequestBody EquipamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar equipamentos operacionais com paginação por cursor", description = "Percorre equipamentos em catálogos extensos usando cursor, útil para inventário incremental, auditoria patrimonial e navegação contínua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<EquipamentoDTO>>>> filterByCursor(@RequestBody EquipamentoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar equipamento operacional em listas filtradas", description = "Informa em qual posição um equipamento aparece dentro do recorte filtrado, útil para reencontrar o ativo em tabelas de inventário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody EquipamentoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar equipamentos operacionais", description = "Retorna o cadastro completo de equipamentos quando o consumidor precisa materializar todos os ativos para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<EquipamentoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar equipamentos operacionais por IDs", description = "Recupera equipamentos já referenciados por alocações, missões ou seleções anteriores sem reaplicar filtros de inventário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<EquipamentoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar equipamentos operacionais para formulários", description = "Produz opções compactas de equipamentos para campos de seleção, autocomplete e vínculos patrimoniais orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody EquipamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar equipamentos operacionais selecionados", description = "Reidrata opções de equipamentos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter equipamento operacional", description = "Retorna o detalhe de um equipamento para inspeção patrimonial, auditoria de uso ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<EquipamentoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @GetMapping("/{id}/allocations")
    @UiSurface(
            id = "equipment-allocation-history",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Historico de custodia",
            description = "Lista alocacoes do equipamento por responsavel, janela e status para auditoria patrimonial, devolucao, perda ou dano.",
            intent = "assets-equipment-allocation-history",
            order = 20,
            tags = {"assets", "equipment", "custody", "audit", "read-projection", "related-resource"},
            relatedChildResourceKey = "assets.equipamento-alocacoes",
            relatedChildResourcePath = ApiPaths.Assets.EQUIPAMENTO_ALOCACOES,
            relatedChildParentField = "equipamentoId",
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
            id = "assets-equipment-allocation-history",
            title = "Cadeia de custodia do equipamento",
            description = "Mostra como um equipamento circulou entre colaboradores, com status de custodia e evidencias para auditoria.",
            order = 20
    )
    @Operation(summary = "Obter alocacoes do equipamento", description = "Retorna o historico de custodia associado ao equipamento selecionado para navegação contextual no cockpit.")
    public ResponseEntity<RestApiResponse<List<EquipamentoAlocacaoDTO>>> getEquipmentAllocations(@PathVariable Integer id) {
        List<EquipamentoAlocacaoDTO> allocations = alocacaoService.findByEquipamentoIdForEquipmentSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/allocations", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(allocations, hateoasOrNull(links)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar equipamento operacional", description = "Cadastra um novo ativo operacional com tipo, capacidade, proprietário e status para uso em campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipamentoDTO>> create(@jakarta.validation.Valid @RequestBody CreateEquipamentoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar equipamento operacional", description = "Atualiza dados do equipamento sem alterar sua identidade, preservando coerência para alocações e fluxos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipamentoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateEquipamentoDTO dto) {
        return super.update(id, dto);
    }

    @PostMapping("/{id}/actions/send-to-maintenance")
    @Operation(summary = "Enviar equipamento para manutenção", description = "Move um equipamento em estoque ou uso para manutenção, removendo sua elegibilidade em novas alocações.")
    @WorkflowAction(
            id = "send-to-maintenance",
            title = "Enviar para manutencao",
            description = "Retira o equipamento da disponibilidade operacional para inspeção, reparo ou bloqueio técnico.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Equipamento enviado para manutenção",
            allowedStates = {"ESTOQUE", "EM_USO"},
            tags = {"workflow", "assets", "equipment", "maintenance"}
    )
    public ResponseEntity<RestApiResponse<AssetAvailabilityWorkflowResultDTO>> sendToMaintenance(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody AssetAvailabilityWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/send-to-maintenance", service.sendToMaintenance(id, dto));
    }

    @PostMapping("/{id}/actions/return-to-stock")
    @Operation(summary = "Devolver equipamento ao estoque", description = "Reabilita um equipamento em manutenção para estoque operacional, tornando-o elegível em novas alocações.")
    @WorkflowAction(
            id = "return-to-stock",
            title = "Devolver ao estoque",
            description = "Libera o equipamento revisado para voltar ao inventario operacional disponível.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Equipamento devolvido ao estoque",
            allowedStates = {"MANUTENCAO"},
            tags = {"workflow", "assets", "equipment", "inventory"}
    )
    public ResponseEntity<RestApiResponse<AssetAvailabilityWorkflowResultDTO>> returnToStock(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody AssetAvailabilityWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/return-to-stock", service.returnToStock(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover equipamento operacional", description = "Exclui um equipamento quando ele deixa de compor o catálogo de ativos disponíveis para operação.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover equipamentos operacionais em lote", description = "Exclui múltiplos equipamentos em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }

    private ResponseEntity<RestApiResponse<AssetAvailabilityWorkflowResultDTO>> workflowResponse(
            Integer id,
            String operationPath,
            AssetAvailabilityWorkflowResultDTO result
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








