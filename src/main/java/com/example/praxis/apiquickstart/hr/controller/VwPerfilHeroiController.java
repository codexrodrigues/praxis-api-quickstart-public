package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.VwPerfilHeroiDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwPerfilHeroiFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwPerfilHeroi;
import com.example.praxis.apiquickstart.hr.mapper.VwPerfilHeroiMapper;
import com.example.praxis.apiquickstart.hr.service.VwPerfilHeroiService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@ApiResource(value = ApiPaths.HumanResources.VW_PERFIL_HEROI, resourceKey = "human-resources.vw-perfil-heroi")
@ApiGroup("human-resources")
@PreAuthorize("@hrDepartmentScopeAccess.isUnscoped(authentication)")
public class VwPerfilHeroiController extends AbstractQuickstartReadOnlyController<VwPerfilHeroi, VwPerfilHeroiDTO, Integer, VwPerfilHeroiFilterDTO> {

    private final VwPerfilHeroiService service;
    private final VwPerfilHeroiMapper mapper;

    @Autowired
    public VwPerfilHeroiController(VwPerfilHeroiService service, VwPerfilHeroiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VwPerfilHeroiService getService() { return service; }

    @Override
    protected VwPerfilHeroiDTO toDto(VwPerfilHeroi entity) { return mapper.toDto(entity); }

    @Override
    protected VwPerfilHeroi toEntity(VwPerfilHeroiDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(VwPerfilHeroi entity) { return entity.getFuncionarioId(); }

    @Override
    protected Integer getDtoId(VwPerfilHeroiDTO dto) { return dto.getFuncionarioId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar perfis 360 por identidade, reputação e contexto operacional", description = "Lista perfis consolidados de funcionários com nome civil, codinome, universo, exposição pública, cargo, departamento, scores reputacionais, habilidades, equipe e base principal para diretórios, descoberta de talentos e visões integradas de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<VwPerfilHeroiDTO>>>> filter(@RequestBody VwPerfilHeroiFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer perfis 360 em listas extensas", description = "Navega por perfis consolidados usando cursor, preservando filtros de identidade, organização, reputação, habilidades, equipe e base em diretórios, buscas incrementais e visões ricas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VwPerfilHeroiDTO>>>> filterByCursor(@RequestBody VwPerfilHeroiFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar perfil 360 dentro de um recorte de descoberta", description = "Informa a posição de um perfil consolidado em lista filtrada por identidade, reputação, habilidade ou contexto operacional para retomada de navegação em tabelas, cards e diretórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VwPerfilHeroiFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar visão completa de perfis 360", description = "Retorna todos os perfis consolidados como visão read-only de identidade, organização, reputação, habilidades e ancoragem operacional para materialização de diretórios e painéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<VwPerfilHeroiDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar perfis 360 por identificadores de funcionário", description = "Recupera perfis consolidados já referenciados em painéis, cards, filtros salvos ou fluxos de seleção usando o identificador do funcionário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VwPerfilHeroiDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar perfis 360 para descoberta e comparação", description = "Produz opções compactas de perfis consolidados para campos de seleção, busca, experiências de descoberta e comparação de talentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VwPerfilHeroiFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de perfis 360 já selecionadas", description = "Reidrata opções de perfis consolidados escolhidas em formulários, diretórios, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de perfil 360", description = "Retorna a visão consolidada de um funcionário com identidade, reputação, habilidades, cargo, departamento, equipe e base para exibição rica, consulta integrada e composição de visões derivadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwPerfilHeroiDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }
}






