package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.ReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.ReputacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Reputacao;
import com.example.praxis.apiquickstart.hr.mapper.ReputacaoMapper;
import com.example.praxis.apiquickstart.hr.service.ReputacaoService;
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

@ApiResource(value = ApiPaths.HumanResources.REPUTACOES, resourceKey = "human-resources.reputacoes")
@ApiGroup("human-resources")
public class ReputacaoController extends AbstractQuickstartCrudController<Reputacao, ReputacaoDTO, Integer, ReputacaoFilterDTO, CreateReputacaoDTO, UpdateReputacaoDTO> {

    private final ReputacaoService service;
    private final ReputacaoMapper mapper;

    @Autowired
    public ReputacaoController(ReputacaoService service, ReputacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected ReputacaoService getService() { return service; }

    @Override
    protected ReputacaoDTO toDto(Reputacao entity) { return mapper.toDto(entity); }

    @Override
    protected Reputacao toEntity(ReputacaoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Reputacao entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(ReputacaoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar reputações por colaborador, scores e atualização", description = "Lista snapshots reputacionais por funcionário, score público, score governamental e data de atualização para rankings, perfis consolidados, visibilidade pública e leitura de frescor dos indicadores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<ReputacaoDTO>>>> filter(@RequestBody ReputacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer reputações em listas extensas", description = "Navega por snapshots reputacionais usando cursor, preservando filtros de colaborador, faixas de score e atualização em rankings, diretórios internos e consultas analíticas extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<ReputacaoDTO>>>> filterByCursor(@RequestBody ReputacaoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar reputação dentro de um recorte comparativo", description = "Informa a posição de um snapshot reputacional em lista filtrada por colaborador, score ou atualização para retomada de navegação em tabelas comparativas e rankings.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody ReputacaoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de reputações", description = "Retorna todos os snapshots reputacionais cadastrados como referência de scores públicos e governamentais para rankings, perfis, exportação, sincronização e análise.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<ReputacaoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar reputações por identificadores conhecidos", description = "Recupera snapshots reputacionais já referenciados em painéis, perfis, filtros salvos ou formulários usando seus identificadores de reputação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<ReputacaoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar reputações para perfis e rankings", description = "Produz opções compactas de reputações para campos de seleção, busca, composição de perfis consolidados e filtros de ranking.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody ReputacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de reputações já selecionadas", description = "Reidrata opções de reputações escolhidas em formulários, perfis, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de reputação", description = "Retorna o snapshot reputacional de um funcionário com score público, score governamental e data de atualização para análise comparativa, perfil consolidado e ranking.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<ReputacaoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar reputação de funcionário", description = "Cria um snapshot reputacional associado a funcionário com scores público e governamental para composição de rankings, perfis e análises de exposição.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<ReputacaoDTO>> create(@jakarta.validation.Valid @RequestBody CreateReputacaoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar reputação de funcionário", description = "Mantém scores público e governamental e data de atualização de um snapshot reputacional usado em perfis, rankings e análises comparativas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<ReputacaoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateReputacaoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover reputação do cadastro", description = "Remove um snapshot reputacional quando a base de rankings e perfis exige saneamento, revisão analítica ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover reputações em lote", description = "Remove várias reputações em uma única chamada para saneamento administrativo, revisão analítica ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









