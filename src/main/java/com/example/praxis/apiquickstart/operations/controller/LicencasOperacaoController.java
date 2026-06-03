package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.LicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.CreateLicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.RenewLicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateLicencasOperacaoDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.LicencasOperacaoFilterDTO;
import com.example.praxis.apiquickstart.operations.entity.LicencasOperacao;
import com.example.praxis.apiquickstart.operations.mapper.LicencasOperacaoMapper;
import com.example.praxis.apiquickstart.operations.service.LicencasOperacaoService;
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
 * Recurso de licencas operacionais usado para demonstrar state-based availability sem action extra.
 *
 * <p>Ao contrario de outros recursos deste dominio, a narrativa principal aqui e a surface parcial
 * de renovacao. O quickstart usa este controller para mostrar que nem toda transicao relevante vira
 * workflow action: algumas cabem melhor como ajuste parcial guiado por estado e schema dedicado.</p>
 */
@ApiResource(value = ApiPaths.Operations.LICENCAS_OPERACAO, resourceKey = "operations.licencas-operacao")
@ApiGroup("operations")
public class LicencasOperacaoController extends AbstractQuickstartCrudController<LicencasOperacao, LicencasOperacaoDTO, Integer, LicencasOperacaoFilterDTO, CreateLicencasOperacaoDTO, UpdateLicencasOperacaoDTO> {

    private final LicencasOperacaoService service;
    private final LicencasOperacaoMapper mapper;

    @Autowired
    public LicencasOperacaoController(LicencasOperacaoService service, LicencasOperacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected LicencasOperacaoService getService() { return service; }

    @Override
    protected LicencasOperacaoDTO toDto(LicencasOperacao entity) { return mapper.toDto(entity); }

    @Override
    protected LicencasOperacao toEntity(LicencasOperacaoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(LicencasOperacao entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(LicencasOperacaoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar licenças de operação", description = "Lista licenças por acordo regulatório, escopo, vigência, responsável e status para comprovar habilitação operacional vigente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<LicencasOperacaoDTO>>>> filter(@RequestBody LicencasOperacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar licenças de operação com paginação por cursor", description = "Percorre licenças em catálogos extensos usando cursor, útil para compliance contínuo, renovação em massa e telas com navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<LicencasOperacaoDTO>>>> filterByCursor(@RequestBody LicencasOperacaoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar licença de operação em listas filtradas", description = "Informa em qual posição uma licença aparece dentro do recorte filtrado, útil para retomar a análise de habilitação em tabelas regulatórias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody LicencasOperacaoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar licenças de operação", description = "Retorna o cadastro completo de licenças quando o consumidor precisa materializar todas as autorizações para conferência, exportação ou sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<LicencasOperacaoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar licenças de operação por IDs", description = "Recupera licenças já referenciadas por missões, equipes ou seleções anteriores sem reaplicar filtros regulatórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<LicencasOperacaoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar licenças de operação para formulários", description = "Produz opções compactas de licenças para campos de seleção, autocomplete e vínculos regulatórios orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody LicencasOperacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar licenças de operação selecionadas", description = "Reidrata opções de licenças já escolhidas em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter licença de operação", description = "Retorna o detalhe de uma licença para inspeção regulatória, auditoria de vigência ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<LicencasOperacaoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar licença de operação", description = "Cadastra uma nova autorização operacional com escopo, vigência e vínculo regulatório para uso em missões e equipes.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<LicencasOperacaoDTO>> create(@jakarta.validation.Valid @RequestBody CreateLicencasOperacaoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar licença de operação", description = "Atualiza dados de uma licença existente sem alterar sua identidade, preservando coerência para fluxos regulatórios dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<LicencasOperacaoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateLicencasOperacaoDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/renew")
    @UiSurface(
            id = "renew",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Renew license",
            description = "Adjust validity window and authorization level for operational continuity",
            intent = "license-renewal",
            order = 30,
            allowedStates = {"ATIVA", "A_EXPIRAR", "EXPIRADA"},
            tags = {"license", "renewal"}
    )
    @ResourceIntent(
            id = "license-renewal",
            title = "Renew operational license",
            description = "Renews validity and scope without recreating the license record",
            order = 30
    )
    @Operation(summary = "Renovar licença de operação", description = "Renova vigência e escopo da licença sem recriar o registro, preservando histórico, rastreabilidade e capabilities do recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Licença renovada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Invalid validity window."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Estado atual não permite renovação.")
    })
    public ResponseEntity<RestApiResponse<LicencasOperacaoDTO>> renew(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody RenewLicencasOperacaoDTO dto
    ) {
        LicencasOperacaoDTO updated = service.renew(id, dto);
        // Mantem a surface de renovacao autodocumentada e reentravel para clientes de schema.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/renew", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(updated, hateoasOrNull(links)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover licença de operação", description = "Exclui uma licença quando ela deixa de compor o catálogo regulatório disponível para operações e vínculos novos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover licenças de operação em lote", description = "Exclui múltiplas licenças em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}












