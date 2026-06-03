package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.SinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateSinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateSinaisSocorroDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.SinaisSocorroFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.SinaisSocorro;
import com.example.praxis.apiquickstart.operations.mapper.SinaisSocorroMapper;
import com.example.praxis.apiquickstart.operations.service.SinaisSocorroService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
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

@RestController
@ApiResource(value = ApiPaths.Operations.SINAIS_SOCORRO, resourceKey = "operations.sinais-socorro")
@ApiGroup("operations")
/**
 * Controller de referência para alertas e sinais de socorro operacionais.
 *
 * <p>No quickstart, este recurso explica como a plataforma hospeda um domínio
 * de alta urgência usando o mesmo contrato de listagem, filtros, seleção e
 * paginação dos demais recursos públicos. Ele é didático porque conecta o
 * padrão genérico de CRUD à semântica de triagem operacional, monitoramento
 * e navegação de catálogos quase em tempo real.</p>
 */
public class SinaisSocorroController extends AbstractQuickstartCrudController<SinaisSocorro, SinaisSocorroDTO, Integer, SinaisSocorroFilterDTO, CreateSinaisSocorroDTO, UpdateSinaisSocorroDTO> {

    private final SinaisSocorroService service;
    private final SinaisSocorroMapper mapper;

    @Autowired
    public SinaisSocorroController(SinaisSocorroService service, SinaisSocorroMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected SinaisSocorroService getService() { return service; }

    @Override
    protected SinaisSocorroDTO toDto(SinaisSocorro entity) { return mapper.toDto(entity); }

    @Override
    protected SinaisSocorro toEntity(SinaisSocorroDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(SinaisSocorro entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(SinaisSocorroDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar sinais de socorro", description = "Lista alertas por nível, origem, missão, base e status para triagem rápida de ameaças e incidentes operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<SinaisSocorroDTO>>>> filter(@RequestBody SinaisSocorroFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar sinais de socorro com paginação por cursor", description = "Percorre alertas em grandes catálogos usando cursor, útil para centrais de monitoramento e navegação incremental em tempo quase real.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<SinaisSocorroDTO>>>> filterByCursor(@RequestBody SinaisSocorroFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar sinal de socorro em listas filtradas", description = "Informa em qual posição um alerta aparece dentro do recorte filtrado, útil para retomar a triagem em tabelas de resposta operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody SinaisSocorroFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar sinais de socorro", description = "Retorna o cadastro completo de alertas quando o consumidor precisa materializar todas as ocorrências para acompanhamento, exportação ou auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<SinaisSocorroDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar sinais de socorro por IDs", description = "Recupera alertas já referenciados por incidentes, relatórios ou seleções anteriores sem reaplicar filtros de origem ou severidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<SinaisSocorroDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar sinais de socorro para formulários", description = "Produz opções compactas de alertas para campos de seleção, lookup e vínculos operacionais orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody SinaisSocorroFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar sinais de socorro selecionados", description = "Reidrata opções de alertas já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter sinal de socorro", description = "Retorna o detalhe de um alerta para triagem operacional, auditoria de resposta e composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<SinaisSocorroDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar sinal de socorro", description = "Cadastra um novo alerta com origem, nível e contexto operacional para disparar acompanhamento e resposta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<SinaisSocorroDTO>> create(@jakarta.validation.Valid @RequestBody CreateSinaisSocorroDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar sinal de socorro", description = "Atualiza dados do alerta sem alterar sua identidade, preservando coerência para incidentes e fluxos analíticos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<SinaisSocorroDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateSinaisSocorroDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover sinal de socorro", description = "Exclui um alerta quando ele não deve mais compor o histórico publicado nem alimentar novos fluxos de resposta.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover sinais de socorro em lote", description = "Exclui múltiplos alertas em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}












