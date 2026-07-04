package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.IncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateIncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateIncidenteDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.IncidenteFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Incidente;
import com.example.praxis.apiquickstart.operations.mapper.IncidenteMapper;
import com.example.praxis.apiquickstart.operations.service.IncidenteService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
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
 * Recurso de incidentes usado como cadastro operacional primario para o dominio de risco aplicado.
 *
 * <p>Ele complementa a camada analitica do quickstart mostrando o lado transacional das ocorrencias
 * de campo. E um bom exemplo de recurso cujo valor pedagogico esta em alimentar views e dashboards
 * posteriores sem perder o baseline CRUD canonico da plataforma.</p>
 */
@ApiResource(
        value = ApiPaths.Operations.INCIDENTES,
        resourceKey = "operations.incidentes",
        title = "Incidentes",
        description = "Ocorrencias operacionais ligadas a missoes, severidade, impacto humano, danos civis e leitura analitica de risco.",
        icon = "siren",
        visualTone = "risk-intelligence"
)
@ApiGroup("operations")
public class IncidenteController extends AbstractQuickstartCrudController<Incidente, IncidenteDTO, Integer, IncidenteFilterDTO, CreateIncidenteDTO, UpdateIncidenteDTO> {

    private final IncidenteService service;
    private final IncidenteMapper mapper;

    @Autowired
    public IncidenteController(IncidenteService service, IncidenteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected IncidenteService getService() { return service; }

    @Override
    protected IncidenteDTO toDto(Incidente entity) { return mapper.toDto(entity); }

    @Override
    protected Incidente toEntity(IncidenteDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Incidente entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(IncidenteDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "incident-investigation-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Mesa de investigacao de incidentes",
            description = "Conecta ocorrencias de missao, severidade, local, danos civis e impacto humano com indenizacoes e a view analitica risk-intelligence.vw-indicadores-incidentes.",
            intent = "operations-incident-risk-investigation",
            order = 35,
            tags = {"operations", "risk-intelligence", "incident", "mission", "severity", "impact", "analytics", "lookup"}
    )
    @Operation(summary = "Filtrar incidentes de missão", description = "Lista incidentes por missão, severidade, impacto, local, data e status para investigação operacional e análise de risco em campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<IncidenteDTO>>>> filter(@RequestBody IncidenteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar incidentes de missão com paginação por cursor", description = "Percorre incidentes em grandes catálogos usando cursor, útil para auditoria contínua, triagem operacional e telas com navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<IncidenteDTO>>>> filterByCursor(@RequestBody IncidenteFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar incidente de missão em listas filtradas", description = "Informa em qual posição um incidente aparece dentro do recorte filtrado, útil para retomar investigações e análises em tabelas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody IncidenteFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar incidentes de missão", description = "Retorna o cadastro completo de incidentes quando o consumidor precisa materializar todas as ocorrências para exportação, reconciliação ou auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<IncidenteDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar incidentes de missão por IDs", description = "Recupera incidentes já associados a relatórios, sinais ou seleções anteriores sem reaplicar filtros de missão ou severidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<IncidenteDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar incidentes de missão para formulários", description = "Produz opções compactas de incidentes para campos de seleção, lookup e vínculos operacionais baseados em busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody IncidenteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar incidentes de missão selecionados", description = "Reidrata opções de incidentes já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter incidente de missão", description = "Retorna o detalhe de um incidente para investigação operacional, auditoria e composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<IncidenteDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar incidente de missão", description = "Cadastra uma nova ocorrência operacional com missão, severidade, impacto e localização para acompanhamento e resposta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<IncidenteDTO>> create(@jakarta.validation.Valid @RequestBody CreateIncidenteDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar incidente de missão", description = "Atualiza dados do incidente sem alterar sua identidade, preservando coerência para relatórios e fluxos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<IncidenteDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateIncidenteDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover incidente de missão", description = "Exclui um incidente quando ele não deve mais compor o histórico publicado nem alimentar novos fluxos analíticos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover incidentes de missão em lote", description = "Exclui múltiplos incidentes em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}











