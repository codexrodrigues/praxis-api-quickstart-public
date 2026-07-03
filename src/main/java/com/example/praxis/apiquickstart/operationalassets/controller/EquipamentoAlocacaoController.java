package com.example.praxis.apiquickstart.operationalassets.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.EquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateEquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateEquipamentoAlocacaoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.EquipamentoAlocacaoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.EquipamentoAlocacao;
import com.example.praxis.apiquickstart.operationalassets.mapper.EquipamentoAlocacaoMapper;
import com.example.praxis.apiquickstart.operationalassets.service.EquipamentoAlocacaoService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.UiSurface;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import org.praxisplatform.uischema.surface.SurfaceKind;
import org.praxisplatform.uischema.surface.SurfaceScope;
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

@ApiResource(
        value = ApiPaths.Assets.EQUIPAMENTO_ALOCACOES,
        resourceKey = "assets.equipamento-alocacoes",
        title = "Alocações de equipamento",
        description = "Cessões, responsáveis, períodos de uso e rastreabilidade de ativos materiais em operação.",
        icon = "package-check",
        visualTone = "assets"
)
@ApiGroup("assets")
/**
 * Controller de referência para distribuição e rastreabilidade de equipamentos.
 *
 * <p>Ele demonstra um recurso relacional de assets no qual o ativo principal e
 * o histórico de cessão convivem com o mesmo padrão de CRUD, filtros e seleção
 * contextual da plataforma. No quickstart, esta classe ajuda a explicar como
 * modelar disponibilidade e posse sem deslocar a lógica para consumidores ou
 * para convenções implícitas fora do backend.</p>
 */
public class EquipamentoAlocacaoController extends AbstractQuickstartCrudController<EquipamentoAlocacao, EquipamentoAlocacaoDTO, Integer, EquipamentoAlocacaoFilterDTO, CreateEquipamentoAlocacaoDTO, UpdateEquipamentoAlocacaoDTO> {

    private final EquipamentoAlocacaoService service;
    private final EquipamentoAlocacaoMapper mapper;

    @Autowired
    public EquipamentoAlocacaoController(EquipamentoAlocacaoService service, EquipamentoAlocacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected EquipamentoAlocacaoService getService() { return service; }

    @Override
    protected EquipamentoAlocacaoDTO toDto(EquipamentoAlocacao entity) { return mapper.toDto(entity); }

    @Override
    protected EquipamentoAlocacao toEntity(EquipamentoAlocacaoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(EquipamentoAlocacao entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(EquipamentoAlocacaoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "equipment-custody-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Cadeia de custodia",
            description = "Rastreia alocacoes de equipamentos por item, responsavel, janela de uso e status da custodia operacional.",
            intent = "assets-equipment-custody",
            order = 20,
            tags = {"assets", "equipment", "custody", "allocation"}
    )
    @Operation(summary = "Filtrar alocações de equipamentos", description = "Lista cessões de equipamentos por colaborador, responsável, período de uso e status para acompanhar distribuição e rastreabilidade dos ativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<EquipamentoAlocacaoDTO>>>> filter(@RequestBody EquipamentoAlocacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar alocações de equipamentos com paginação por cursor", description = "Percorre alocações em catálogos extensos usando cursor, útil para auditoria patrimonial, inventário contínuo e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<EquipamentoAlocacaoDTO>>>> filterByCursor(@RequestBody EquipamentoAlocacaoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar alocação de equipamento em listas filtradas", description = "Informa em qual posição uma alocação aparece dentro do recorte filtrado, útil para retomar a análise de cessão em tabelas patrimoniais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody EquipamentoAlocacaoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar alocações de equipamentos", description = "Retorna o cadastro completo das cessões quando o consumidor precisa materializar todo o histórico de distribuição de equipamentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<EquipamentoAlocacaoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar alocações de equipamentos por IDs", description = "Recupera alocações já referenciadas por outro fluxo sem reaplicar filtros de equipamento, colaborador ou período.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<EquipamentoAlocacaoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar alocações de equipamentos para formulários", description = "Produz opções compactas de alocações para lookup e seleção contextual em fluxos patrimoniais e operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody EquipamentoAlocacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar alocações de equipamentos selecionadas", description = "Reidrata opções de alocações já escolhidas em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter alocação de equipamento", description = "Retorna o detalhe de uma cessão para inspeção patrimonial, auditoria de uso ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<EquipamentoAlocacaoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar alocação de equipamento", description = "Cadastra a cessão de um equipamento para uso operacional, registrando responsável, período e contexto do vínculo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipamentoAlocacaoDTO>> create(@jakarta.validation.Valid @RequestBody CreateEquipamentoAlocacaoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar alocação de equipamento", description = "Atualiza dados da cessão sem alterar sua identidade, preservando coerência para auditoria e fluxos patrimoniais dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<EquipamentoAlocacaoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateEquipamentoAlocacaoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover alocação de equipamento", description = "Exclui uma cessão quando ela não deve mais compor o histórico patrimonial publicado nem alimentar novos fluxos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover alocações de equipamentos em lote", description = "Exclui múltiplas cessões em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}










