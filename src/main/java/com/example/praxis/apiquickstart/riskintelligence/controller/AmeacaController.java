package com.example.praxis.apiquickstart.riskintelligence.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.riskintelligence.dto.AmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.CreateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.UpdateAmeacaDTO;
import com.example.praxis.apiquickstart.riskintelligence.dto.filter.AmeacaFilterDTO;
import com.example.praxis.apiquickstart.riskintelligence.entity.Ameaca;
import com.example.praxis.apiquickstart.riskintelligence.mapper.AmeacaMapper;
import com.example.praxis.apiquickstart.riskintelligence.service.AmeacaService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
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
 * Recurso de ameacas usado como entrada transacional do dominio de risco.
 *
 * <p>Ele mostra como a plataforma trata o catalogo de risco monitorado como recurso canonico,
 * separado das views analiticas derivadas. Isso ajuda a distinguir, no quickstart, o que e dado
 * operacional primario e o que e leitura agregada para inteligencia.</p>
 */
@ApiResource(
        value = ApiPaths.RiskIntelligence.AMEACAS,
        resourceKey = "risk-intelligence.ameacas",
        title = "Ameaças",
        description = "Riscos monitorados, classificação, nível, origem, status e sinais para priorização analítica.",
        icon = "shield-alert",
        visualTone = "risk-intelligence"
)
@ApiGroup("risk-intelligence")
public class AmeacaController extends AbstractQuickstartCrudController<Ameaca, AmeacaDTO, Integer, AmeacaFilterDTO, CreateAmeacaDTO, UpdateAmeacaDTO> {

    private final AmeacaService service;
    private final AmeacaMapper mapper;

    @Autowired
    public AmeacaController(AmeacaService service, AmeacaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected AmeacaService getService() { return service; }

    @Override
    protected AmeacaDTO toDto(Ameaca entity) { return mapper.toDto(entity); }

    @Override
    protected Ameaca toEntity(AmeacaDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Ameaca entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(AmeacaDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar ameaças monitoradas", description = "Lista ameaças por classificação, nível, status, origem e risco associado para triagem de risco, monitoramento contínuo e priorização em painéis de inteligência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<AmeacaDTO>>>> filter(@RequestBody AmeacaFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar ameaças monitoradas com paginação por cursor", description = "Percorre ameaças em catálogos extensos usando cursor, útil para watchlists, centrais analíticas e navegação incremental em superfícies de risco.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<AmeacaDTO>>>> filterByCursor(@RequestBody AmeacaFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar ameaça monitorada em listas filtradas", description = "Informa em qual posição uma ameaça aparece dentro do recorte filtrado, útil para retornar ao item em painéis de monitoramento e tabelas de risco.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody AmeacaFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar ameaças monitoradas", description = "Retorna o catálogo completo de ameaças quando o consumidor precisa materializar toda a base de risco para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<AmeacaDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar ameaças monitoradas por IDs", description = "Recupera ameaças já referenciadas por incidentes, painéis ou seleções anteriores sem reaplicar filtros de risco.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<AmeacaDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar ameaças monitoradas para formulários", description = "Produz opções compactas de ameaças para filtros salvos, seleção guiada e vínculos com incidentes, sinais ou painéis analíticos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody AmeacaFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar ameaças monitoradas selecionadas", description = "Reidrata ameaças já escolhidas em filtros, painéis e vínculos analíticos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter ameaça monitorada", description = "Retorna o detalhe de uma ameaça para inspeção analítica, auditoria de classificação ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<AmeacaDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar ameaça monitorada", description = "Cadastra um novo item de risco com classificação, nível e status para monitoramento e correlação analítica.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<AmeacaDTO>> create(@jakarta.validation.Valid @RequestBody CreateAmeacaDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar ameaça monitorada", description = "Atualiza dados da ameaça sem alterar sua identidade, preservando coerência para incidentes e fluxos analíticos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<AmeacaDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateAmeacaDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover ameaça monitorada", description = "Exclui uma ameaça quando ela deixa de compor o catálogo de risco monitorado e não deve mais aparecer em novos fluxos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover ameaças monitoradas em lote", description = "Exclui múltiplas ameaças em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









