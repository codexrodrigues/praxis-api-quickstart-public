package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.MencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateMencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateMencoesMidiaDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.MencoesMidiaFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.MencoesMidia;
import com.example.praxis.apiquickstart.hr.mapper.MencoesMidiaMapper;
import com.example.praxis.apiquickstart.hr.service.MencoesMidiaService;
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

@ApiResource(value = ApiPaths.HumanResources.MENCOES_MIDIA, resourceKey = "human-resources.mencoes-midia")
@ApiGroup("human-resources")
public class MencoesMidiaController extends AbstractQuickstartCrudController<MencoesMidia, MencoesMidiaDTO, Integer, MencoesMidiaFilterDTO, CreateMencoesMidiaDTO, UpdateMencoesMidiaDTO> {

    private final MencoesMidiaService service;
    private final MencoesMidiaMapper mapper;

    @Autowired
    public MencoesMidiaController(MencoesMidiaService service, MencoesMidiaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected MencoesMidiaService getService() { return service; }

    @Override
    protected MencoesMidiaDTO toDto(MencoesMidia entity) { return mapper.toDto(entity); }

    @Override
    protected MencoesMidia toEntity(MencoesMidiaDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(MencoesMidia entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(MencoesMidiaDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar menções de mídia por colaborador, veículo e sentimento", description = "Lista publicações e citações associadas a funcionários por veículo, sentimento, URL e período de publicação para monitoramento reputacional, análise de exposição pública e resposta institucional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<MencoesMidiaDTO>>>> filter(@RequestBody MencoesMidiaFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer menções de mídia em grandes volumes", description = "Navega por menções de mídia usando cursor, preservando filtros de colaborador, veículo, sentimento, URL e período em monitoramento contínuo e consultas reputacionais extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<MencoesMidiaDTO>>>> filterByCursor(@RequestBody MencoesMidiaFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar menção de mídia dentro de um recorte reputacional", description = "Informa a posição de uma menção em lista filtrada por colaborador, veículo, sentimento ou período para retomada de navegação em tabelas de monitoramento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody MencoesMidiaFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de menções de mídia", description = "Retorna todas as menções monitoradas como referência de exposição pública, sentimento e histórico reputacional para conferência, exportação, sincronização e análise.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<MencoesMidiaDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar menções de mídia por identificadores conhecidos", description = "Recupera menções já referenciadas em perfis, relatórios, filtros salvos ou painéis usando seus identificadores de monitoramento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<MencoesMidiaDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar menções de mídia para fluxos reputacionais", description = "Produz opções compactas de menções de mídia para campos de seleção, busca, filtros de reputação e vinculação com análises de exposição pública.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody MencoesMidiaFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de menções de mídia já selecionadas", description = "Reidrata opções de menções escolhidas em formulários, relatórios, filtros salvos ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de menção de mídia", description = "Retorna uma menção com colaborador, veículo, sentimento, URL e data de publicação para consulta da fonte, contexto de exposição pública e análise reputacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<MencoesMidiaDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar menção de mídia", description = "Cria uma evidência de mídia associada a funcionário com veículo, sentimento, URL e data de publicação para monitoramento reputacional e histórico de exposição.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MencoesMidiaDTO>> create(@jakarta.validation.Valid @RequestBody CreateMencoesMidiaDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar menção de mídia", description = "Mantém veículo, sentimento, URL, data de publicação e vínculo com funcionário para monitoramento reputacional, relatórios e análises de exposição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MencoesMidiaDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateMencoesMidiaDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover menção de mídia", description = "Remove uma menção do cadastro de monitoramento quando o histórico reputacional exige saneamento, revisão editorial ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover menções de mídia em lote", description = "Remove várias menções em uma única chamada para saneamento administrativo, revisão reputacional ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









