package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.HabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HabilidadeFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Habilidade;
import com.example.praxis.apiquickstart.hr.mapper.HabilidadeMapper;
import com.example.praxis.apiquickstart.hr.service.HabilidadeService;
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

@ApiResource(value = ApiPaths.HumanResources.HABILIDADES, resourceKey = "human-resources.habilidades")
@ApiGroup("human-resources")
public class HabilidadeController extends AbstractQuickstartCrudController<Habilidade, HabilidadeDTO, Integer, HabilidadeFilterDTO, CreateHabilidadeDTO, UpdateHabilidadeDTO> {

    private final HabilidadeService service;
    private final HabilidadeMapper mapper;

    @Autowired
    public HabilidadeController(HabilidadeService service, HabilidadeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected HabilidadeService getService() { return service; }

    @Override
    protected HabilidadeDTO toDto(Habilidade entity) { return mapper.toDto(entity); }

    @Override
    protected Habilidade toEntity(HabilidadeDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Habilidade entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(HabilidadeDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar catálogo de habilidades por categoria e nível de poder", description = "Lista habilidades do catálogo por nome, categoria, descrição e nível de poder para compor perfis profissionais, matrizes de competência, filtros de talento e recomendações de equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<HabilidadeDTO>>>> filter(@RequestBody HabilidadeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer catálogo de habilidades em listas extensas", description = "Navega por habilidades usando cursor, preservando filtros de nome, categoria, descrição e nível de poder em diretórios de competências, seletores de perfil e telas de associação a funcionários.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<HabilidadeDTO>>>> filterByCursor(@RequestBody HabilidadeFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar habilidade dentro de um recorte do catálogo", description = "Informa a posição de uma habilidade em lista filtrada por categoria, descrição ou nível de poder para retomada de navegação em catálogos, seletores e matrizes de competência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody HabilidadeFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar catálogo completo de habilidades", description = "Retorna todas as habilidades cadastradas como repertório de competências e poderes para materializar diretórios de talentos, matrizes de habilidade e análises de alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<HabilidadeDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar habilidades por identificadores conhecidos", description = "Recupera habilidades já referenciadas em perfis, formulários, filtros salvos ou associações com funcionários usando seus identificadores do catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<HabilidadeDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar habilidades para perfis e filtros", description = "Produz opções compactas de habilidades para campos de seleção, busca, montagem de perfis profissionais e filtros de alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody HabilidadeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de habilidades já selecionadas", description = "Reidrata opções de habilidades escolhidas em formulários, perfis, filtros salvos ou painéis, preservando identificador e rótulo do catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de habilidade do catálogo", description = "Retorna a definição de uma habilidade com nome, categoria, descrição e nível de poder para consulta de capacidade, composição de perfis e regras de elegibilidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<HabilidadeDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar habilidade no catálogo de competências", description = "Cria uma habilidade com nome, categoria, descrição e nível de poder para uso em perfis profissionais, associação a funcionários e recomendações de equipe.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<HabilidadeDTO>> create(@jakarta.validation.Valid @RequestBody CreateHabilidadeDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar habilidade do catálogo de competências", description = "Mantém nome, categoria, descrição e nível de poder de uma habilidade usada em perfis profissionais, associações com funcionários e recomendações de alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<HabilidadeDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateHabilidadeDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover habilidade do catálogo de competências", description = "Remove uma habilidade do catálogo administrativo quando a matriz de competências exige saneamento, revisão de classificação ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover habilidades do catálogo em lote", description = "Remove várias habilidades em uma única operação para saneamento administrativo, revisão de matriz de competências ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









