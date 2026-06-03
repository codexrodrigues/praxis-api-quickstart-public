package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.BaseDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateBaseDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseOpsContextDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.BaseFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Base;
import com.example.praxis.apiquickstart.operations.mapper.BaseMapper;
import com.example.praxis.apiquickstart.operations.service.BaseService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import java.util.List;

/**
 * Recurso de bases operacionais usado para demonstrar contexto tatico parcial sobre um ativo fixo.
 *
 * <p>Ele e um exemplo util de recurso cujo ajuste mais frequente nao exige workflow, mas sim uma
 * surface parcial para atualizar contexto operacional como sigilo e localizacao sem recriar o
 * cadastro principal.</p>
 */
@ApiResource(value = ApiPaths.Operations.BASES, resourceKey = "operations.bases")
@ApiGroup("operations")
public class BaseController extends AbstractQuickstartCrudController<Base, BaseDTO, Integer, BaseFilterDTO, CreateBaseDTO, UpdateBaseDTO> {

    private final BaseService service;
    private final BaseMapper mapper;

    @Autowired
    public BaseController(BaseService service, BaseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected BaseService getService() { return service; }

    @Override
    protected BaseDTO toDto(Base entity) { return mapper.toDto(entity); }

    @Override
    protected Base toEntity(BaseDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Base entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(BaseDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar bases operacionais", description = "Lista bases por nome, tipo, localização, status logístico e nível de sigilo para planejamento operacional, distribuição e apoio de campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<BaseDTO>>>> filter(@RequestBody BaseFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar bases operacionais com paginação por cursor", description = "Percorre bases em catálogos extensos usando cursor, útil para dashboards logísticos, discovery incremental e telas com rolagem contínua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<BaseDTO>>>> filterByCursor(@RequestBody BaseFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar base operacional em listas filtradas", description = "Informa em qual posição uma base aparece dentro do recorte filtrado, útil para reencontrar o registro em tabelas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody BaseFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar bases operacionais", description = "Retorna o cadastro completo de bases quando o consumidor precisa materializar todo o catálogo para exportação, conferência ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<BaseDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar bases operacionais por IDs", description = "Recupera bases já referenciadas por missões, acessos ou seleções anteriores sem reaplicar filtros de busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<BaseDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar bases operacionais para formulários", description = "Produz opções compactas de bases para campos de seleção, autocomplete e vínculos operacionais orientados por filtro textual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody BaseFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar bases operacionais selecionadas", description = "Reidrata opções de bases já escolhidas em formulários e filtros salvos, preservando rótulo e identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter base operacional", description = "Retorna o detalhe de uma base para inspeção operacional, auditoria de cadastro ou montagem de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<BaseDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar base operacional", description = "Cadastra uma nova base com metadados logísticos e de sigilo para uso em planejamento, acesso e alocação operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<BaseDTO>> create(@jakarta.validation.Valid @RequestBody CreateBaseDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar base operacional", description = "Atualiza dados estruturais da base sem alterar sua identidade, mantendo coerência para consumidores que dependem do cadastro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<BaseDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateBaseDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/ops-context")
    @UiSurface(
            id = "ops-context",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Update ops context",
            description = "Adjust security and map context for operational planning",
            intent = "base-ops-context",
            order = 30,
            tags = {"base", "ops", "map"}
    )
    @ResourceIntent(
            id = "base-ops-context",
            title = "Update base ops context",
            description = "Adjusts security profile and map context for the selected base",
            order = 30
    )
    @Operation(summary = "Atualizar contexto operacional da base", description = "Ajusta sigilo, localização e contexto tático da base sem recriar o cadastro principal nem perder o histórico associado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contexto operacional atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<BaseDTO>> updateOpsContext(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody UpdateBaseOpsContextDTO dto
    ) {
        BaseDTO updated = service.updateOpsContext(id, dto);
        // Mantem a surface parcial redescobrivel para clientes que consomem schemas do host.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/ops-context", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(updated, hateoasOrNull(links)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover base operacional", description = "Exclui uma base quando ela deixa de compor o catálogo operacional e não deve mais aparecer em novas consultas ou vínculos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover bases operacionais em lote", description = "Exclui múltiplas bases em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}












