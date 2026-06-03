package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.CreateFuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.FuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioHabilidadeDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FuncionarioHabilidadeFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FuncionarioHabilidade;
import com.example.praxis.apiquickstart.hr.mapper.FuncionarioHabilidadeMapper;
import com.example.praxis.apiquickstart.hr.service.FuncionarioHabilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;

@ApiResource(value = ApiPaths.HumanResources.FUNCIONARIO_HABILIDADES, resourceKey = "human-resources.funcionario-habilidades")
@ApiGroup("human-resources")
public class FuncionarioHabilidadeController extends AbstractQuickstartCrudController<FuncionarioHabilidade, FuncionarioHabilidadeDTO, Integer, FuncionarioHabilidadeFilterDTO, CreateFuncionarioHabilidadeDTO, UpdateFuncionarioHabilidadeDTO> {

    private final FuncionarioHabilidadeService service;
    private final FuncionarioHabilidadeMapper mapper;

    @Autowired
    public FuncionarioHabilidadeController(FuncionarioHabilidadeService service, FuncionarioHabilidadeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected FuncionarioHabilidadeService getService() { return service; }

    @Override
    protected FuncionarioHabilidadeDTO toDto(FuncionarioHabilidade entity) { return mapper.toDto(entity); }

    @Override
    protected FuncionarioHabilidade toEntity(FuncionarioHabilidadeDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(FuncionarioHabilidade entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(FuncionarioHabilidadeDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar matriz de habilidades por funcionário, competência e proficiência", description = "Lista associações entre funcionários e habilidades por colaborador, habilidade, nível de proficiência e origem da capacitação para perfis profissionais, busca de talentos, elegibilidade e alocação em equipes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<FuncionarioHabilidadeDTO>>>> filter(@RequestBody FuncionarioHabilidadeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer matriz de habilidades em grandes volumes", description = "Navega por associações funcionário-habilidade usando cursor, preservando filtros de colaborador, habilidade, proficiência e origem em diretórios de talentos, matrizes de competência e consultas operacionais extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<FuncionarioHabilidadeDTO>>>> filterByCursor(@RequestBody FuncionarioHabilidadeFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar habilidade de funcionário dentro de um recorte de competências", description = "Informa a posição de uma associação funcionário-habilidade em lista filtrada por colaborador, habilidade, proficiência ou origem para retomada de navegação em tabelas de perfil e matrizes de competência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody FuncionarioHabilidadeFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar matriz completa de habilidades dos funcionários", description = "Retorna todas as associações entre funcionários e habilidades como referência de repertório profissional para materializar mapas de competência, diretórios de talentos e análises de alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<FuncionarioHabilidadeDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar vínculos de habilidade por identificadores conhecidos", description = "Recupera associações funcionário-habilidade já referenciadas em formulários, painéis, filtros salvos ou fluxos de edição usando seus identificadores da matriz de competências.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<FuncionarioHabilidadeDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar habilidades de funcionários para perfis e filtros", description = "Produz opções compactas de associações funcionário-habilidade para campos de seleção, busca, montagem de perfil profissional e filtros de alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody FuncionarioHabilidadeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de habilidades de funcionários já selecionadas", description = "Reidrata opções de associações funcionário-habilidade escolhidas em formulários, perfis, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de habilidade vinculada ao funcionário", description = "Retorna a associação entre funcionário e habilidade com proficiência e origem da capacitação para análise de repertório, composição de perfil profissional e decisões de alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioHabilidadeDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar habilidade vinculada ao funcionário", description = "Cria uma associação entre funcionário e habilidade do catálogo com proficiência e origem da capacitação para refletir repertório profissional e apoiar alocação.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioHabilidadeDTO>> create(@jakarta.validation.Valid @RequestBody CreateFuncionarioHabilidadeDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar habilidade vinculada ao funcionário", description = "Mantém proficiência, origem da capacitação e relação entre funcionário e habilidade para perfis profissionais, recomendações de alocação e matrizes de competência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<FuncionarioHabilidadeDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateFuncionarioHabilidadeDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover habilidade do perfil do funcionário", description = "Remove uma associação funcionário-habilidade da matriz de competências quando o perfil profissional exige saneamento, revisão de capacitação ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover habilidades de funcionários em lote", description = "Remove várias associações funcionário-habilidade em uma única chamada para saneamento administrativo, revisão de matriz de competências ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}
