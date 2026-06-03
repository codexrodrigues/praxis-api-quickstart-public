package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.MissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateMissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateMissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.MissaoParticipanteFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.MissaoParticipante;
import com.example.praxis.apiquickstart.operations.mapper.MissaoParticipanteMapper;
import com.example.praxis.apiquickstart.operations.service.MissaoParticipanteService;
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

@ApiResource(value = ApiPaths.Operations.MISSAO_PARTICIPANTES, resourceKey = "operations.missao-participantes")
@ApiGroup("operations")
/**
 * Controller de referência para a composição humana de uma missão.
 *
 * <p>Este recurso mostra como o quickstart modela vínculos entre missão,
 * participante, papel e período de atuação sem sair do padrão de CRUD
 * metadata-driven. Ele serve como exemplo público de recurso relacional
 * cujo valor didático está em demonstrar alocação operacional, filtros
 * contextuais e seleção incremental para formulários e tabelas.</p>
 */
public class MissaoParticipanteController extends AbstractQuickstartCrudController<MissaoParticipante, MissaoParticipanteDTO, Integer, MissaoParticipanteFilterDTO, CreateMissaoParticipanteDTO, UpdateMissaoParticipanteDTO> {

    private final MissaoParticipanteService service;
    private final MissaoParticipanteMapper mapper;

    @Autowired
    public MissaoParticipanteController(MissaoParticipanteService service, MissaoParticipanteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected MissaoParticipanteService getService() { return service; }

    @Override
    protected MissaoParticipanteDTO toDto(MissaoParticipante entity) { return mapper.toDto(entity); }

    @Override
    protected MissaoParticipante toEntity(MissaoParticipanteDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(MissaoParticipante entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(MissaoParticipanteDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar participantes de missão", description = "Lista vínculos de participantes por missão, papel, status e período para acompanhar alocação operacional e composição de campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<MissaoParticipanteDTO>>>> filter(@RequestBody MissaoParticipanteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar participantes de missão com paginação por cursor", description = "Percorre vínculos de participantes em catálogos extensos usando cursor, útil para escalas de missão e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<MissaoParticipanteDTO>>>> filterByCursor(@RequestBody MissaoParticipanteFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar participante de missão em listas filtradas", description = "Informa em qual posição um vínculo aparece dentro do recorte filtrado, útil para retomar a análise de alocação em tabelas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody MissaoParticipanteFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar participantes de missão", description = "Retorna o cadastro completo de participantes alocados quando o consumidor precisa materializar a composição integral de missões.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<MissaoParticipanteDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar participantes de missão por IDs", description = "Recupera vínculos já referenciados por outro fluxo sem reaplicar filtros de missão, pessoa ou papel.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<MissaoParticipanteDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar participantes de missão para formulários", description = "Produz opções compactas de vínculos de participante para formulários, lookup e seleção contextual em missões.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody MissaoParticipanteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar participantes de missão selecionados", description = "Reidrata opções de vínculos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter participante de missão", description = "Retorna o detalhe de um vínculo de participante para inspeção de papel, vigência e contexto operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<MissaoParticipanteDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar participante de missão", description = "Cadastra a associação entre missão e participante com papel, período e contexto de alocação operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MissaoParticipanteDTO>> create(@jakarta.validation.Valid @RequestBody CreateMissaoParticipanteDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar participante de missão", description = "Atualiza papel e metadados do vínculo sem alterar sua identidade na composição da missão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MissaoParticipanteDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateMissaoParticipanteDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover participante de missão", description = "Exclui o vínculo quando o participante não deve mais compor a missão nem aparecer em novas consultas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover participantes de missão em lote", description = "Exclui múltiplos vínculos em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}












