package com.example.praxis.apiquickstart.operationalassets.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.actions.AssetAvailabilityWorkflowResultDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.VeiculoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import com.example.praxis.apiquickstart.operationalassets.mapper.VeiculoMapper;
import com.example.praxis.apiquickstart.operationalassets.service.VeiculoService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.praxisplatform.uischema.action.ActionScope;
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

/**
 * Recurso de veiculos usado como referencia de ativos operacionais no quickstart.
 *
 * <p>Ele mostra como um dominio patrimonial e logistico entra no mesmo baseline metadata-driven do
 * restante da plataforma: CRUD canonico, options, locate, cursor e hypermedia sem necessidade de
 * tratamento especial por ser um ativo fisico.</p>
 */
@ApiResource(
        value = ApiPaths.Assets.VEICULOS,
        resourceKey = "assets.veiculos",
        title = "Veiculos",
        description = "Frota operacional, disponibilidade, capacidade, custodiante e elegibilidade para uso em missoes.",
        icon = "local-shipping",
        visualTone = "assets"
)
@ApiGroup("assets")
public class VeiculoController extends AbstractQuickstartCrudController<Veiculo, VeiculoDTO, Integer, VeiculoFilterDTO, CreateVeiculoDTO, UpdateVeiculoDTO> {

    private final VeiculoService service;
    private final VeiculoMapper mapper;

    @Autowired
    public VeiculoController(VeiculoService service, VeiculoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VeiculoService getService() { return service; }

    @Override
    protected VeiculoDTO toDto(Veiculo entity) { return mapper.toDto(entity); }

    @Override
    protected Veiculo toEntity(VeiculoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Veiculo entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(VeiculoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "fleet-readiness-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Prontidao da frota",
            description = "Mostra veiculos por tipo, capacidade, custodiante e status para decidir disponibilidade logistica de missoes.",
            intent = "assets-fleet-readiness",
            order = 30,
            tags = {"assets", "vehicle", "fleet", "mission"}
    )
    @Operation(summary = "Filtrar veículos operacionais", description = "Lista veículos por tipo, capacidade, proprietário, disponibilidade e status para planejamento logístico, transporte e apoio de campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<VeiculoDTO>>>> filter(@RequestBody VeiculoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar veículos operacionais com paginação por cursor", description = "Percorre veículos em catálogos extensos usando cursor, útil para inventário móvel, dashboards logísticos e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VeiculoDTO>>>> filterByCursor(@RequestBody VeiculoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar veículo operacional em listas filtradas", description = "Informa em qual posição um veículo aparece dentro do recorte filtrado, útil para reencontrar o ativo em tabelas de frota.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VeiculoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar veículos operacionais", description = "Retorna o cadastro completo da frota quando o consumidor precisa materializar todos os ativos para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<VeiculoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar veículos operacionais por IDs", description = "Recupera veículos já referenciados por missões, alocações ou seleções anteriores sem reaplicar filtros de frota.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VeiculoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar veículos operacionais para formulários", description = "Produz opções compactas de veículos para campos de seleção, autocomplete e vínculos logísticos orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VeiculoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar veículos operacionais selecionados", description = "Reidrata opções de veículos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter veículo operacional", description = "Retorna o detalhe de um veículo para inspeção de frota, auditoria patrimonial ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VeiculoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar veículo operacional", description = "Cadastra um novo ativo de transporte com capacidade, proprietário e status para uso em logística e missões.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<VeiculoDTO>> create(@jakarta.validation.Valid @RequestBody CreateVeiculoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo operacional", description = "Atualiza dados do veículo sem alterar sua identidade, preservando coerência para alocações e fluxos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<VeiculoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateVeiculoDTO dto) {
        return super.update(id, dto);
    }

    @PostMapping("/{id}/actions/send-to-maintenance")
    @Operation(summary = "Enviar veículo para manutenção", description = "Move um veículo operacional para manutenção, removendo sua elegibilidade em novas missões e usos de frota.")
    @WorkflowAction(
            id = "send-to-maintenance",
            title = "Enviar para manutencao",
            description = "Retira o veículo da frota operacional para inspeção, reparo ou bloqueio logístico.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Veículo enviado para manutenção",
            allowedStates = {"OPERACIONAL"},
            tags = {"workflow", "assets", "vehicle", "maintenance"}
    )
    public ResponseEntity<RestApiResponse<AssetAvailabilityWorkflowResultDTO>> sendToMaintenance(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody AssetAvailabilityWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/send-to-maintenance", service.sendToMaintenance(id, dto));
    }

    @PostMapping("/{id}/actions/return-to-operation")
    @Operation(summary = "Liberar veículo para operação", description = "Reabilita um veículo em manutenção ou inoperante para uso operacional em missões.")
    @WorkflowAction(
            id = "return-to-operation",
            title = "Liberar para operacao",
            description = "Devolve o veículo revisado à frota operacional disponível.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Veículo liberado para operação",
            allowedStates = {"MANUTENCAO", "INOPERANTE"},
            tags = {"workflow", "assets", "vehicle", "fleet"}
    )
    public ResponseEntity<RestApiResponse<AssetAvailabilityWorkflowResultDTO>> returnToOperation(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody AssetAvailabilityWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/return-to-operation", service.returnToOperation(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover veículo operacional", description = "Exclui um veículo quando ele deixa de compor o catálogo de ativos disponíveis para transporte e apoio operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover veículos operacionais em lote", description = "Exclui múltiplos veículos em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
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










