package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.BaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateBaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.ReviewBaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateBaseAcessoDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.BaseAcessoWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.BaseAcessoWorkflowResultDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.BaseAcessoFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.BaseAcesso;
import com.example.praxis.apiquickstart.operations.mapper.BaseAcessoMapper;
import com.example.praxis.apiquickstart.operations.service.BaseAcessoService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.action.ActionScope;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
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
 * Recurso de acesso a bases usado para demonstrar governanca operacional de permissao.
 *
 * <p>Ele combina review parcial de nivel de acesso com workflow de ativacao/desativacao do vinculo.
 * Isso ajuda a mostrar, no quickstart, a diferenca entre editar metadados de um acesso e publicar a
 * mudanca do seu estado operacional.</p>
 */
@RestController
@ApiResource(value = ApiPaths.Operations.BASE_ACESSOS, resourceKey = "operations.base-acessos")
@ApiGroup("operations")
public class BaseAcessoController extends AbstractQuickstartCrudController<BaseAcesso, BaseAcessoDTO, Integer, BaseAcessoFilterDTO, CreateBaseAcessoDTO, UpdateBaseAcessoDTO> {

    private final BaseAcessoService service;
    private final BaseAcessoMapper mapper;

    @Autowired
    public BaseAcessoController(BaseAcessoService service, BaseAcessoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected BaseAcessoService getService() { return service; }

    @Override
    protected BaseAcessoDTO toDto(BaseAcesso entity) { return mapper.toDto(entity); }

    @Override
    protected BaseAcesso toEntity(BaseAcessoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(BaseAcesso entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(BaseAcessoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar acessos a bases operacionais", description = "Lista acessos por base, colaborador, nível de autorização, vigência e status para auditoria de entrada e governança operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<BaseAcessoDTO>>>> filter(@RequestBody BaseAcessoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar acessos a bases operacionais com paginação por cursor", description = "Percorre grandes volumes de permissões de acesso com cursor, útil para auditoria contínua, catálogos extensos e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<BaseAcessoDTO>>>> filterByCursor(@RequestBody BaseAcessoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar acesso a base operacional em listas filtradas", description = "Informa em qual posição um acesso aparece dentro do recorte filtrado, útil para retomar a análise de um vínculo específico em tabelas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody BaseAcessoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar acessos a bases operacionais", description = "Retorna o cadastro completo de acessos quando o consumidor precisa materializar todas as permissões para conferência, exportação ou reconciliação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<BaseAcessoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar acessos a bases operacionais por IDs", description = "Recupera permissões já referenciadas por outro fluxo sem reaplicar filtros de base, pessoa ou status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<BaseAcessoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar acessos a bases operacionais para formulários", description = "Produz opções compactas de acessos para formulários administrativos, lookup e vínculos que partem de permissões já filtradas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody BaseAcessoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar acessos a bases operacionais selecionados", description = "Reidrata opções de acessos já escolhidos em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter acesso a base operacional", description = "Retorna o detalhe de uma permissão para inspeção de auditoria, revisão de autorização ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<BaseAcessoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar acesso a base operacional", description = "Cadastra uma nova permissão de entrada relacionando pessoa, base, nível de autorização e vigência operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<BaseAcessoDTO>> create(@jakarta.validation.Valid @RequestBody CreateBaseAcessoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar acesso a base operacional", description = "Atualiza os dados de autorização de um vínculo existente sem perder sua identidade nem o histórico de auditoria associado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<BaseAcessoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateBaseAcessoDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/review-access")
    @UiSurface(
            id = "review-access",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Review access",
            description = "Adjusts access level while preserving the access record",
            intent = "base-access-review",
            order = 30,
            allowedStates = {"ATIVO", "INATIVO"},
            tags = {"access", "review"}
    )
    @ResourceIntent(
            id = "base-access-review",
            title = "Review base access",
            description = "Adjusts the access level for the selected employee and base",
            order = 30
    )
    @Operation(summary = "Revisar autorização de acesso à base", description = "Ajusta nível de acesso e metadados de autorização preservando o histórico do vínculo entre pessoa e base.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autorização revisada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<BaseAcessoDTO>> reviewAccess(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody ReviewBaseAcessoDTO dto
    ) {
        BaseAcessoDTO reviewed = service.reviewAccess(id, dto);
        // Mantem a surface parcial autodocumentada para ajustes administrativos de acesso.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/review-access", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(reviewed, hateoasOrNull(links)));
    }

    @PostMapping("/{id}/actions/activate")
    @Operation(summary = "Ativar acesso à base", description = "Ativa o vínculo de acesso e publica a transição para catálogos de ações, capabilities e auditoria operacional.")
    @WorkflowAction(
            id = "activate",
            title = "Ativar acesso",
            description = "Ativa a credencial de acesso de uma pessoa a base operacional selecionada.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Acesso ativado",
            allowedStates = {"INATIVO"},
            tags = {"workflow", "access"}
    )
    public ResponseEntity<RestApiResponse<BaseAcessoWorkflowResultDTO>> activate(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody BaseAcessoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/activate", service.activate(id, dto));
    }

    @PostMapping("/{id}/actions/deactivate")
    @Operation(summary = "Desativar acesso à base", description = "Desativa o vínculo de acesso preservando o contexto de auditoria e a rastreabilidade da autorização concedida.")
    @WorkflowAction(
            id = "deactivate",
            title = "Desativar acesso",
            description = "Desativa a credencial de acesso a base mantendo o contexto necessario para auditoria.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Acesso desativado",
            allowedStates = {"ATIVO"},
            tags = {"workflow", "access"}
    )
    public ResponseEntity<RestApiResponse<BaseAcessoWorkflowResultDTO>> deactivate(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody BaseAcessoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/deactivate", service.deactivate(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover acesso a base operacional", description = "Exclui uma permissão quando ela não deve mais compor o catálogo operacional nem ser reutilizada em novos fluxos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover acessos a bases operacionais em lote", description = "Exclui múltiplas permissões em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }

    private ResponseEntity<RestApiResponse<BaseAcessoWorkflowResultDTO>> workflowResponse(
            Integer id,
            String operationPath,
            BaseAcessoWorkflowResultDTO result
    ) {
        // Cada action reapresenta os links do recurso e dos schemas da transicao de acesso.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema(operationPath, "post", "request"),
                linkToUiSchema(operationPath, "post", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(result, hateoasOrNull(links)));
    }
}












