package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.DepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateDepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateDepartamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.DepartamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Departamento;
import com.example.praxis.apiquickstart.hr.mapper.DepartamentoMapper;
import com.example.praxis.apiquickstart.hr.service.DepartamentoService;
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
@ApiResource(value = ApiPaths.HumanResources.DEPARTAMENTOS, resourceKey = "human-resources.departamentos")
@ApiGroup("human-resources")
public class DepartamentoController extends AbstractQuickstartCrudController<Departamento, DepartamentoDTO, Integer, DepartamentoFilterDTO, CreateDepartamentoDTO, UpdateDepartamentoDTO> {

    private final DepartamentoService service;
    private final DepartamentoMapper mapper;

    @Autowired
    public DepartamentoController(DepartamentoService service, DepartamentoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected DepartamentoService getService() {
        return service;
    }

    @Override
    protected DepartamentoDTO toDto(Departamento entity) { return mapper.toDto(entity); }

    @Override
    protected Departamento toEntity(DepartamentoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Departamento entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(DepartamentoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar departamentos por nome, código e responsável", description = "Lista unidades organizacionais de RH por nome, código interno e funcionário responsável para lotação, organização funcional, navegação em catálogos e seleção em formulários corporativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<DepartamentoDTO>>>> filter(@RequestBody DepartamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer catálogo de departamentos em listas extensas", description = "Navega por departamentos usando cursor, preservando filtros por unidade, código interno e responsável em tabelas de lotação, seletores organizacionais e catálogos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<DepartamentoDTO>>>> filterByCursor(@RequestBody DepartamentoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar departamento dentro de um recorte organizacional", description = "Informa a posição de um departamento em uma lista filtrada por nome, código ou responsável para retomada de navegação em catálogos, formulários e painéis de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody DepartamentoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar catálogo completo de departamentos", description = "Retorna todos os departamentos cadastrados como referência de lotação e organização funcional para conferência administrativa, exportação, sincronização de catálogos e composição de telas internas de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<DepartamentoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar departamentos por identificadores conhecidos", description = "Recupera departamentos já referenciados em vínculos de funcionários, relatórios, filtros salvos ou seleções anteriores usando seus identificadores do catálogo organizacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<DepartamentoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar departamentos para lotação e filtros de RH", description = "Produz opções compactas de departamentos para campos de seleção, autocomplete, filtros de estrutura organizacional e composição de vínculos de funcionários.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody DepartamentoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de departamentos já selecionadas", description = "Reidrata opções de departamentos escolhidas em formulários, lotações, filtros salvos ou painéis, preservando identificador e rótulo de exibição do catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de departamento", description = "Retorna a definição de um departamento com nome, código interno e responsável para inspeção do catálogo organizacional, manutenção administrativa e composição de visões dependentes de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<DepartamentoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar departamento no catálogo organizacional", description = "Cria uma unidade organizacional com nome, código interno e responsável para uso em lotação, vínculos de funcionários, relatórios e filtros de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<DepartamentoDTO>> create(@jakarta.validation.Valid @RequestBody CreateDepartamentoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar definição de departamento", description = "Mantém nome, código interno e responsável de um departamento usado em lotações, vínculos de funcionários, relatórios e estruturas organizacionais de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<DepartamentoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateDepartamentoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover departamento do catálogo organizacional", description = "Remove uma unidade organizacional do catálogo administrativo quando a estrutura de RH exige saneamento, revisão de dados ou retirada de departamentos obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover departamentos do catálogo organizacional em lote", description = "Remove múltiplas unidades organizacionais em uma única chamada para saneamento administrativo, revisão de estrutura funcional ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









