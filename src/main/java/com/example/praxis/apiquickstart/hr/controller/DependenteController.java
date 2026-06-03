package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.CreateDependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.DependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateDependenteDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.DependenteFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Dependente;
import com.example.praxis.apiquickstart.hr.mapper.DependenteMapper;
import com.example.praxis.apiquickstart.hr.service.DependenteService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;

@RestController
@ApiResource(value = ApiPaths.HumanResources.DEPENDENTES, resourceKey = "human-resources.dependentes")
@ApiGroup("human-resources")
public class DependenteController extends AbstractQuickstartCrudController<Dependente, DependenteDTO, Integer, DependenteFilterDTO, CreateDependenteDTO, UpdateDependenteDTO> {

    private final DependenteService service;
    private final DependenteMapper mapper;

    @Autowired
    public DependenteController(DependenteService service, DependenteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected DependenteService getService() { return service; }

    @Override
    protected DependenteDTO toDto(Dependente entity) { return mapper.toDto(entity); }

    @Override
    protected Dependente toEntity(DependenteDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Dependente entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(DependenteDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar dependentes por titular, parentesco e nascimento", description = "Lista vínculos familiares declarados por funcionário, nome do dependente, grau de parentesco e data de nascimento para conferência cadastral, elegibilidade de benefícios e atendimento administrativo de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<DependenteDTO>>>> filter(@RequestBody DependenteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer cadastro de dependentes em listas extensas", description = "Navega por dependentes usando cursor, preservando filtros por titular, parentesco e nascimento em cadastros familiares, tabelas de benefícios e fluxos administrativos de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<DependenteDTO>>>> filterByCursor(@RequestBody DependenteFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar dependente dentro de um recorte familiar", description = "Informa a posição de um dependente em uma lista filtrada por titular, parentesco ou nascimento para retomada de navegação em cadastros familiares e tabelas de benefícios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody DependenteFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de dependentes", description = "Retorna todos os dependentes cadastrados como referência de vínculos familiares para conferência administrativa, exportação, sincronização e composição de telas internas de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<DependenteDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar dependentes por identificadores conhecidos", description = "Recupera dependentes já referenciados em benefícios, formulários, filtros salvos ou seleções anteriores usando seus identificadores cadastrais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<DependenteDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar dependentes para benefícios e vínculos administrativos", description = "Produz opções compactas de dependentes para campos de seleção, busca, filtros de benefícios e fluxos administrativos vinculados ao funcionário titular.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody DependenteFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de dependentes já selecionadas", description = "Reidrata opções de dependentes escolhidas em formulários, benefícios, filtros salvos ou atendimentos de RH, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de dependente", description = "Retorna o cadastro de um dependente com titular, nome civil, parentesco e data de nascimento para inspeção cadastral, análise de elegibilidade e composição de visões familiares de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<DependenteDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar dependente vinculado a funcionário", description = "Cria um vínculo familiar declarado com funcionário titular, nome civil, parentesco e data de nascimento para uso em benefícios, conferência cadastral e registros administrativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<DependenteDTO>> create(@jakarta.validation.Valid @RequestBody CreateDependenteDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cadastro de dependente", description = "Mantém dados cadastrais do dependente, parentesco, data de nascimento e vínculo com o funcionário titular para benefícios, conferência familiar e atendimento administrativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<DependenteDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateDependenteDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover dependente do cadastro familiar", description = "Remove um vínculo familiar do cadastro administrativo quando a base de dependentes exige saneamento, revisão cadastral ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover dependentes do cadastro familiar em lote", description = "Remove múltiplos vínculos familiares em uma única chamada para saneamento administrativo, revisão cadastral ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}
