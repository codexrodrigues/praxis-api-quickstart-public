package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.HistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateHistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateHistoricosCargoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.HistoricosCargoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.HistoricosCargo;
import com.example.praxis.apiquickstart.hr.mapper.HistoricosCargoMapper;
import com.example.praxis.apiquickstart.hr.service.HistoricosCargoService;
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

@ApiResource(value = ApiPaths.HumanResources.HISTORICOS_CARGOS, resourceKey = "human-resources.historicos-cargos")
@ApiGroup("human-resources")
public class HistoricosCargoController extends AbstractQuickstartCrudController<HistoricosCargo, HistoricosCargoDTO, Integer, HistoricosCargoFilterDTO, CreateHistoricosCargoDTO, UpdateHistoricosCargoDTO> {

    private final HistoricosCargoService service;
    private final HistoricosCargoMapper mapper;

    @Autowired
    public HistoricosCargoController(HistoricosCargoService service, HistoricosCargoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected HistoricosCargoService getService() { return service; }

    @Override
    protected HistoricosCargoDTO toDto(HistoricosCargo entity) { return mapper.toDto(entity); }

    @Override
    protected HistoricosCargo toEntity(HistoricosCargoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(HistoricosCargo entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(HistoricosCargoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar trilha de cargos por colaborador, cargo e vigência", description = "Lista mudanças funcionais por funcionário, cargo, data de início e data de fim para trilha de carreira, mobilidade interna, lotação histórica e auditoria funcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<HistoricosCargoDTO>>>> filter(@RequestBody HistoricosCargoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer históricos de cargos em listas extensas", description = "Navega por registros de cargo usando cursor, preservando filtros de colaborador, cargo e vigência em trilhas funcionais, auditorias de carreira e consultas operacionais extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<HistoricosCargoDTO>>>> filterByCursor(@RequestBody HistoricosCargoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar registro de cargo dentro de um recorte histórico", description = "Informa a posição de um histórico de cargo em lista filtrada por colaborador, cargo ou vigência para retomada de navegação em tabelas de carreira e auditoria funcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody HistoricosCargoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar trilha completa de cargos", description = "Retorna todos os históricos de cargos cadastrados como referência de trajetória funcional para conferência administrativa, auditoria, exportação, sincronização e análise de carreira.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<HistoricosCargoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar históricos de cargos por identificadores conhecidos", description = "Recupera registros funcionais já referenciados em relatórios, análises, filtros salvos ou seleções anteriores usando seus identificadores da trilha de cargos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<HistoricosCargoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar históricos de cargos para carreira e auditoria", description = "Produz opções compactas de registros funcionais para seleção contextual em formulários, auditorias, análises de carreira e composição de dados de lotação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody HistoricosCargoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de históricos de cargos já selecionadas", description = "Reidrata opções de registros funcionais escolhidas em formulários, auditorias, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de histórico de cargo", description = "Retorna um registro da trilha funcional com colaborador, cargo, vigência e observações para inspeção administrativa, auditoria de carreira e composição de visões de RH.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<HistoricosCargoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar registro na trilha de cargos", description = "Cria um registro funcional com colaborador, cargo, vigência e observações para representar promoção, transferência, alocação ou mudança de função.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<HistoricosCargoDTO>> create(@jakarta.validation.Valid @RequestBody CreateHistoricosCargoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro da trilha de cargos", description = "Mantém cargo, vigência e observações de um registro funcional usado em relatórios, auditorias, análises de carreira e estruturas de lotação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<HistoricosCargoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateHistoricosCargoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover registro da trilha de cargos", description = "Remove um histórico funcional do cadastro administrativo quando a trilha de carreira exige saneamento, revisão de lotação ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover históricos de cargos em lote", description = "Remove múltiplos registros funcionais em uma única chamada para saneamento administrativo, revisão de carreira ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









