package com.example.praxis.apiquickstart.operationalassets.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateVeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateVeiculoMissaoUsoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.VeiculoMissaoUsoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.VeiculoMissaoUso;
import com.example.praxis.apiquickstart.operationalassets.mapper.VeiculoMissaoUsoMapper;
import com.example.praxis.apiquickstart.operationalassets.service.VeiculoMissaoUsoService;
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

/**
 * Recurso de uso de veiculos em missao usado para demonstrar vinculos operacionais entre dominios.
 *
 * <p>Ele e pedagogicamente util porque conecta dois agregados importantes do quickstart
 * ({@code assets.veiculos} e {@code operations.missoes}) sem sair do baseline resource-oriented da
 * plataforma. Isso ajuda a mostrar como modelar relacoes operacionais sem criar endpoints
 * excepcionais.</p>
 */
@ApiResource(
        value = ApiPaths.Assets.VEICULO_MISSAO_USOS,
        resourceKey = "assets.veiculo-missao-usos",
        title = "Uso de veiculos em missao",
        description = "Sorties e usos de frota conectando veiculos, missoes, pilotos, partida, retorno e observacoes operacionais.",
        icon = "route",
        visualTone = "assets"
)
@ApiGroup("assets")
public class VeiculoMissaoUsoController extends AbstractQuickstartCrudController<VeiculoMissaoUso, VeiculoMissaoUsoDTO, Integer, VeiculoMissaoUsoFilterDTO, CreateVeiculoMissaoUsoDTO, UpdateVeiculoMissaoUsoDTO> {

    private final VeiculoMissaoUsoService service;
    private final VeiculoMissaoUsoMapper mapper;

    @Autowired
    public VeiculoMissaoUsoController(VeiculoMissaoUsoService service, VeiculoMissaoUsoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected VeiculoMissaoUsoService getService() { return service; }

    @Override
    protected VeiculoMissaoUsoDTO toDto(VeiculoMissaoUso entity) { return mapper.toDto(entity); }

    @Override
    protected VeiculoMissaoUso toEntity(VeiculoMissaoUsoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(VeiculoMissaoUso entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(VeiculoMissaoUsoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @UiSurface(
            id = "mission-fleet-usage-board",
            kind = SurfaceKind.VIEW,
            scope = SurfaceScope.COLLECTION,
            title = "Frota em missao",
            description = "Conecta veiculos a missoes, pilotos e janelas de uso para rastrear disponibilidade e historico logistico.",
            intent = "assets-mission-fleet-usage",
            order = 40,
            tags = {"assets", "vehicle", "mission", "timeline"}
    )
    @Operation(summary = "Filtrar uso de veículos em missão", description = "Lista alocações de veículos por missão, piloto, período de uso e status para acompanhar disponibilidade e consumo da frota em campo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<VeiculoMissaoUsoDTO>>>> filter(@RequestBody VeiculoMissaoUsoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar uso de veículos em missão com paginação por cursor", description = "Percorre vínculos de uso de veículos em catálogos extensos usando cursor, útil para timelines logísticas e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<VeiculoMissaoUsoDTO>>>> filterByCursor(@RequestBody VeiculoMissaoUsoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar uso de veículo em missão em listas filtradas", description = "Informa em qual posição uma alocação de veículo aparece dentro do recorte filtrado, útil para retomar a análise da frota em missão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody VeiculoMissaoUsoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar uso de veículos em missão", description = "Retorna o cadastro completo das alocações de veículos quando o consumidor precisa materializar todo o histórico logístico de missões.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<VeiculoMissaoUsoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar uso de veículos em missão por IDs", description = "Recupera alocações já referenciadas por outro fluxo sem reaplicar filtros de missão, veículo ou período.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<VeiculoMissaoUsoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar uso de veículos em missão para formulários", description = "Produz opções compactas de alocações para lookup e seleção contextual em fluxos logísticos e operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody VeiculoMissaoUsoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar uso de veículos em missão selecionados", description = "Reidrata opções de alocações já escolhidas em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter uso de veículo em missão", description = "Retorna o detalhe de uma alocação de veículo para inspeção logística, auditoria de uso ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VeiculoMissaoUsoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar uso de veículo em missão", description = "Cadastra a associação entre missão, veículo, piloto e janela de uso para planejamento e rastreabilidade da frota.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<VeiculoMissaoUsoDTO>> create(@jakarta.validation.Valid @RequestBody CreateVeiculoMissaoUsoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uso de veículo em missão", description = "Atualiza dados da alocação sem alterar sua identidade, preservando coerência para relatórios e fluxos logísticos dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<VeiculoMissaoUsoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateVeiculoMissaoUsoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover uso de veículo em missão", description = "Exclui uma alocação quando ela não deve mais compor o histórico logístico publicado nem alimentar novos fluxos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover uso de veículos em missão em lote", description = "Exclui múltiplas alocações em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}











