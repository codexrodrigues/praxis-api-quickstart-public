package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.HistoricoSalarialDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHistoricoSalarialDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHistoricoSalarialDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HistoricoSalarialFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.HistoricoSalarial;
import com.example.praxis.apiquickstart.hr.mapper.HistoricoSalarialMapper;
import com.example.praxis.apiquickstart.hr.service.HistoricoSalarialService;
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

@ApiResource(value = ApiPaths.HumanResources.HISTORICOS_SALARIAIS, resourceKey = "human-resources.historicos-salariais")
@ApiGroup("human-resources")
public class HistoricoSalarialController extends AbstractQuickstartCrudController<HistoricoSalarial, HistoricoSalarialDTO, Integer, HistoricoSalarialFilterDTO, CreateHistoricoSalarialDTO, UpdateHistoricoSalarialDTO> {

    private final HistoricoSalarialService service;
    private final HistoricoSalarialMapper mapper;

    @Autowired
    public HistoricoSalarialController(HistoricoSalarialService service, HistoricoSalarialMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected HistoricoSalarialService getService() { return service; }

    @Override
    protected HistoricoSalarialDTO toDto(HistoricoSalarial entity) { return mapper.toDto(entity); }

    @Override
    protected HistoricoSalarial toEntity(HistoricoSalarialDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(HistoricoSalarial entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(HistoricoSalarialDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar trilha salarial por colaborador, valor e vigência", description = "Lista registros de evolução remuneratória por funcionário, faixa salarial, início de vigência, fim de vigência e motivo para auditoria financeira, progressão de carreira e conferência de histórico salarial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<HistoricoSalarialDTO>>>> filter(@RequestBody HistoricoSalarialFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer históricos salariais em listas extensas", description = "Navega por registros salariais usando cursor, preservando filtros de colaborador, valores, vigência e motivo em trilhas de remuneração, auditorias e consultas operacionais extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<HistoricoSalarialDTO>>>> filterByCursor(@RequestBody HistoricoSalarialFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar registro salarial dentro de um recorte histórico", description = "Informa a posição de um histórico salarial em lista filtrada por colaborador, valor, vigência ou motivo para retomada de navegação em tabelas de auditoria e carreira.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody HistoricoSalarialFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar trilha salarial completa", description = "Retorna todos os históricos salariais cadastrados como referência de evolução remuneratória para conferência financeira, auditoria, exportação, sincronização e análise de carreira.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<HistoricoSalarialDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar históricos salariais por identificadores conhecidos", description = "Recupera registros salariais já referenciados em análises, relatórios, filtros salvos ou seleções anteriores usando seus identificadores da trilha remuneratória.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<HistoricoSalarialDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar históricos salariais para auditoria e carreira", description = "Produz opções compactas de registros salariais para seleção contextual em formulários, auditorias, análises e composição de dados de carreira.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody HistoricoSalarialFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de históricos salariais já selecionadas", description = "Reidrata opções de registros salariais escolhidas em formulários, auditorias, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de histórico salarial", description = "Retorna um registro da trilha salarial com funcionário, valor, vigência e motivo para inspeção financeira, auditoria, análise de carreira e composição de visões de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<HistoricoSalarialDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar registro na trilha salarial", description = "Cria um registro de remuneração com funcionário, valor, vigência e motivo para representar progressão, reajuste ou correção na trajetória salarial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<HistoricoSalarialDTO>> create(@jakarta.validation.Valid @RequestBody CreateHistoricoSalarialDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro da trilha salarial", description = "Mantém valor, vigência e motivo de um registro salarial usado em relatórios, auditorias financeiras, análises de carreira e preparação de folha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<HistoricoSalarialDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateHistoricoSalarialDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover registro da trilha salarial", description = "Remove um histórico salarial do cadastro administrativo quando a trilha remuneratória exige saneamento, revisão financeira ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover históricos salariais em lote", description = "Remove múltiplos registros salariais em uma única chamada para saneamento administrativo, revisão financeira ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









