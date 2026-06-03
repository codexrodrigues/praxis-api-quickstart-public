package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.VwRankingReputacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwRankingReputacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwRankingReputacao;
import com.example.praxis.apiquickstart.hr.mapper.VwRankingReputacaoMapper;
import com.example.praxis.apiquickstart.hr.service.VwRankingReputacaoService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartReadOnlyController;
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

@ApiResource(value = ApiPaths.HumanResources.VW_RANKING_REPUTACAO, resourceKey = "human-resources.vw-ranking-reputacao")
@ApiGroup("human-resources")
public class VwRankingReputacaoController extends AbstractQuickstartReadOnlyController<VwRankingReputacao, VwRankingReputacaoDTO, Integer, VwRankingReputacaoFilterDTO> {

    private final VwRankingReputacaoService service;
    private final VwRankingReputacaoMapper mapper;

    @Autowired
    public VwRankingReputacaoController(VwRankingReputacaoService service, VwRankingReputacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwRankingReputacaoService getService() { return service; }

    @Override
    protected VwRankingReputacaoDTO toDto(VwRankingReputacao entity) { return mapper.toDto(entity); }

    @Override
    protected VwRankingReputacao toEntity(VwRankingReputacaoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(VwRankingReputacao entity) { return entity.getFuncionarioId(); }

    @Override
    protected Integer getDtoId(VwRankingReputacaoDTO dto) { return dto.getFuncionarioId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar ranking de reputação por colaborador, equipe e scores", description = "Lista a visão comparativa de reputação por funcionário, codinome, equipe, score público, score governamental, média composta e posição para rankings, comparação de perfis e acompanhamento de exposição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<VwRankingReputacaoDTO>>>> filter(@RequestBody VwRankingReputacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer ranking de reputação em listas extensas", description = "Navega por registros de ranking usando cursor, preservando filtros de colaborador, codinome, equipe, scores, média e posição em tabelas comparativas, diretórios ordenados e painéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VwRankingReputacaoDTO>>>> filterByCursor(@RequestBody VwRankingReputacaoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar posição em ranking de reputação filtrado", description = "Informa a posição de um funcionário no ranking filtrado por equipe, scores, média ou faixa ordinal para retomada de navegação em comparações e painéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VwRankingReputacaoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar visão completa do ranking de reputação", description = "Retorna todos os registros da visão read-only de ranking reputacional para materializar base comparativa, diretórios ordenados, exportações e painéis executivos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<VwRankingReputacaoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar registros de ranking por identificadores de funcionário", description = "Recupera linhas de ranking já referenciadas em painéis, seleções, filtros salvos ou comparações usando o identificador do funcionário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VwRankingReputacaoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar registros de ranking para comparações guiadas", description = "Produz opções compactas do ranking para campos de seleção, busca, comparação de perfis e filtros de painéis reputacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VwRankingReputacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de ranking já selecionadas", description = "Reidrata opções de ranking escolhidas em formulários, comparações, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de ranking de reputação", description = "Retorna a linha de ranking de um funcionário com codinome, equipe, scores, média composta e posição para leitura comparativa, análise reputacional e composição de painéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwRankingReputacaoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }
}







