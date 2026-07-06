package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.EquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateEquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.EquipeMembroDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateEquipeDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.EquipeFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.Equipe;
import com.example.praxis.apiquickstart.operations.mapper.EquipeMapper;
import com.example.praxis.apiquickstart.operations.service.EquipeMembroService;
import com.example.praxis.apiquickstart.operations.service.EquipeService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.praxisplatform.uischema.surface.RelatedResourceChildOperation;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.praxisplatform.uischema.rest.response.RestApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
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

@ApiResource(
        value = ApiPaths.Operations.EQUIPES,
        resourceKey = "operations.equipes",
        title = "Equipes operacionais",
        description = "Unidades táticas responsáveis por cobertura, escala, prontidão e composição de capacidade para missões.",
        icon = "users",
        visualTone = "operations"
)
@ApiGroup("operations")
/**
 * Controller de referência para equipes operacionais.
 *
 * <p>Ele demonstra como o quickstart expõe uma unidade organizacional do domínio
 * operacional mantendo o mesmo contrato HTTP e metadata-driven usado em recursos
 * mais simples. Como projeto público de exemplo, esta classe ajuda a mostrar
 * como equipes podem ser filtradas, localizadas e reaproveitadas por option
 * sources em fluxos de missão, base e alocação.</p>
 */
public class EquipeController extends AbstractQuickstartCrudController<Equipe, EquipeDTO, Integer, EquipeFilterDTO, CreateEquipeDTO, UpdateEquipeDTO> {

    private final EquipeService service;
    private final EquipeMapper mapper;
    private final EquipeMembroService membroService;

    @Autowired
    public EquipeController(EquipeService service, EquipeMapper mapper, EquipeMembroService membroService) {
        this.service = service;
        this.mapper = mapper;
        this.membroService = membroService;
    }

    @Override
    protected EquipeService getService() { return service; }

    @Override
    protected EquipeDTO toDto(Equipe entity) { return mapper.toDto(entity); }

    @Override
    protected Equipe toEntity(EquipeDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Equipe entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(EquipeDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar equipes operacionais", description = "Lista equipes por base de referência, status, responsabilidade e composição para planejamento tático e distribuição de trabalho.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<EquipeDTO>>>> filter(@RequestBody EquipeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar equipes operacionais com paginação por cursor", description = "Percorre equipes em catálogos extensos usando cursor, útil para escalas operacionais, lookup incremental e tabelas com rolagem contínua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<EquipeDTO>>>> filterByCursor(@RequestBody EquipeFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar equipe operacional em listas filtradas", description = "Informa em qual posição uma equipe aparece dentro do recorte filtrado, útil para retornar ao registro em tabelas de alocação e missão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody EquipeFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar equipes operacionais", description = "Retorna o cadastro completo de equipes quando o consumidor precisa materializar toda a estrutura operacional para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<EquipeDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar equipes operacionais por IDs", description = "Recupera equipes já associadas a missões, bases ou seleções anteriores sem reaplicar filtros de composição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<EquipeDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar equipes operacionais para formulários", description = "Produz opções compactas de equipes para campos de seleção, autocomplete e vínculos operacionais orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody EquipeFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar equipes operacionais selecionadas", description = "Reidrata opções de equipes já escolhidas em formulários e filtros salvos, preservando rótulo e identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter equipe operacional", description = "Retorna o detalhe de uma equipe para inspeção de composição, responsabilidade e uso em surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<EquipeDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @GetMapping("/{id}/members")
    @UiSurface(
            id = "members",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Composição da equipe",
            description = "Lista membros, papéis e vigências da equipe para leitura de capacidade operacional, cobertura, sucessão e escala.",
            intent = "team-composition",
            order = 40,
            tags = {"operations", "team", "members", "staffing", "capacity", "read-projection", "related-resource"},
            relatedChildResourceKey = "operations.equipe-membros",
            relatedChildResourcePath = ApiPaths.Operations.EQUIPE_MEMBROS,
            relatedChildParentField = "equipeId",
            relatedSelectable = true,
            relatedSelectionKeyField = "id",
            relatedChildOperations = {
                    RelatedResourceChildOperation.FILTER,
                    RelatedResourceChildOperation.LIST,
                    RelatedResourceChildOperation.CREATE,
                    RelatedResourceChildOperation.UPDATE,
                    RelatedResourceChildOperation.DELETE
            }
    )
    @ResourceIntent(
            id = "team-composition",
            title = "Composição operacional da equipe",
            description = "Mostra quem compõe a equipe, quais papéis estão cobertos e qual vigência sustenta a capacidade operacional.",
            order = 40
    )
    @Operation(summary = "Obter composição da equipe", description = "Retorna vínculos de membros da equipe selecionada para leitura de capacidade, escala e responsabilidade operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Composição retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<EquipeMembroDTO>>> getMembers(@PathVariable Integer id) {
        List<EquipeMembroDTO> members = membroService.findByEquipeIdForTeamSurface(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/members", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(members, hateoasOrNull(links)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar equipe operacional", description = "Cadastra uma nova equipe com base de referência, status e responsabilidades para uso em missões e escalas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipeDTO>> create(@jakarta.validation.Valid @RequestBody CreateEquipeDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar equipe operacional", description = "Atualiza composição e metadados da equipe sem alterar sua identidade no catálogo operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipeDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateEquipeDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover equipe operacional", description = "Exclui uma equipe quando ela deixa de compor a estrutura operacional ativa e não deve mais ser alocada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover equipes operacionais em lote", description = "Exclui múltiplas equipes em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}










