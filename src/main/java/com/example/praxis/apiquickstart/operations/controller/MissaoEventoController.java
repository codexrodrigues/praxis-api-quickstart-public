package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.MissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateMissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateMissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.MissaoEventoFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.MissaoEvento;
import com.example.praxis.apiquickstart.operations.mapper.MissaoEventoMapper;
import com.example.praxis.apiquickstart.operations.service.MissaoEventoService;
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

@ApiResource(value = ApiPaths.Operations.MISSAO_EVENTOS, resourceKey = "operations.missao-eventos")
@ApiGroup("operations")
/**
 * Controller de referência para o recurso de linha do tempo de missão.
 *
 * <p>No quickstart, este endpoint demonstra como a plataforma expõe um recurso
 * relacional e cronológico via CRUD metadata-driven, mantendo contratos de filtro,
 * paginação, locate e option source consistentes com os demais recursos públicos.
 * Ele é útil para explicar que a semântica operacional da missão não fica apenas
 * no agregado principal: eventos intermediários também compõem surfaces e fluxos
 * de acompanhamento orientados por estado.</p>
 */
public class MissaoEventoController extends AbstractQuickstartCrudController<MissaoEvento, MissaoEventoDTO, Integer, MissaoEventoFilterDTO, CreateMissaoEventoDTO, UpdateMissaoEventoDTO> {

    private final MissaoEventoService service;
    private final MissaoEventoMapper mapper;

    @Autowired
    public MissaoEventoController(MissaoEventoService service, MissaoEventoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected MissaoEventoService getService() { return service; }

    @Override
    protected MissaoEventoDTO toDto(MissaoEvento entity) { return mapper.toDto(entity); }

    @Override
    protected MissaoEvento toEntity(MissaoEventoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(MissaoEvento entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(MissaoEventoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar eventos de missão", description = "Lista eventos por missão, tipo, momento e relevância para reconstruir a linha do tempo operacional e apoiar acompanhamento em campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<MissaoEventoDTO>>>> filter(@RequestBody MissaoEventoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar eventos de missão com paginação por cursor", description = "Percorre eventos em catálogos extensos usando cursor, útil para timelines operacionais, auditoria e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<MissaoEventoDTO>>>> filterByCursor(@RequestBody MissaoEventoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar evento de missão em listas filtradas", description = "Informa em qual posição um evento aparece dentro do recorte filtrado, útil para retornar ao ponto certo da linha do tempo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody MissaoEventoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar eventos de missão", description = "Retorna o cadastro completo de eventos quando o consumidor precisa materializar toda a linha do tempo operacional para análise ou exportação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<MissaoEventoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar eventos de missão por IDs", description = "Recupera eventos já referenciados por outro fluxo sem reaplicar filtros de missão, tipo ou período.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<MissaoEventoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar eventos de missão para formulários", description = "Produz opções compactas de eventos para campos de seleção, lookup e vínculos que partem de uma busca operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody MissaoEventoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar eventos de missão selecionados", description = "Reidrata opções de eventos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter evento de missão", description = "Retorna o detalhe de um evento para inspeção da linha do tempo, auditoria ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<MissaoEventoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar evento de missão", description = "Cadastra um novo marco operacional associado à missão para registrar progresso, mudança de contexto ou ocorrência relevante.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MissaoEventoDTO>> create(@jakarta.validation.Valid @RequestBody CreateMissaoEventoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar evento de missão", description = "Atualiza o conteúdo de um evento sem alterar sua identidade na linha do tempo operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MissaoEventoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateMissaoEventoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover evento de missão", description = "Exclui um evento quando ele não deve mais compor a linha do tempo publicada nem alimentar novos fluxos analíticos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover eventos de missão em lote", description = "Exclui múltiplos eventos em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}












