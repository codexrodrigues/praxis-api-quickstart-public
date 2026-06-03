package com.example.praxis.apiquickstart.hr.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.hr.dto.IndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateIndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateIndenizacaoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.IndenizacaoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Indenizacao;
import com.example.praxis.apiquickstart.hr.mapper.IndenizacaoMapper;
import com.example.praxis.apiquickstart.hr.service.IndenizacaoService;
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

@ApiResource(value = ApiPaths.HumanResources.INDENIZACOES, resourceKey = "human-resources.indenizacoes")
@ApiGroup("human-resources")
public class IndenizacaoController extends AbstractQuickstartCrudController<Indenizacao, IndenizacaoDTO, Integer, IndenizacaoFilterDTO, CreateIndenizacaoDTO, UpdateIndenizacaoDTO> {

    private final IndenizacaoService service;
    private final IndenizacaoMapper mapper;

    @Autowired
    public IndenizacaoController(IndenizacaoService service, IndenizacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected IndenizacaoService getService() { return service; }

    @Override
    protected IndenizacaoDTO toDto(Indenizacao entity) { return mapper.toDto(entity); }

    @Override
    protected Indenizacao toEntity(IndenizacaoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Indenizacao entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(IndenizacaoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar indenizações por incidente, pagamento e valor", description = "Lista coberturas indenizatórias por incidente operacional, situação de pagamento, faixa de valor, seguradora e protocolo de processo para acompanhamento financeiro, ressarcimentos e conciliação de sinistros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<IndenizacaoDTO>>>> filter(@RequestBody IndenizacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Percorrer indenizações em grandes volumes", description = "Navega por indenizações usando cursor, preservando filtros de incidente, pagamento, valor, seguradora e processo em rotinas financeiras, auditorias e consultas operacionais extensas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<IndenizacaoDTO>>>> filterByCursor(@RequestBody IndenizacaoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar indenização dentro de um recorte financeiro", description = "Informa a posição de uma indenização em lista filtrada por incidente, pagamento, valor, seguradora ou processo para retomada de navegação em tabelas financeiras e analíticas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody IndenizacaoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar cadastro completo de indenizações", description = "Retorna todas as indenizações cadastradas como referência de coberturas, pagamentos, seguradoras e processos vinculados a incidentes para conferência, exportação e sincronização.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<IndenizacaoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar indenizações por identificadores conhecidos", description = "Recupera indenizações já referenciadas em formulários, relatórios, filtros salvos ou fluxos de pagamento usando seus identificadores de cobertura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<IndenizacaoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar indenizações para fluxos financeiros", description = "Produz opções compactas de indenizações para campos de seleção, busca, filtros de sinistro e vinculação com rotinas financeiras ou operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody IndenizacaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar opções de indenizações já selecionadas", description = "Reidrata opções de indenizações escolhidas em formulários, filtros salvos, relatórios ou painéis, preservando identificador e rótulo de exibição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhe de indenização", description = "Retorna uma cobertura indenizatória com incidente, valor, situação de pagamento, seguradora e processo para conferência financeira, auditoria de pagamento e consulta administrativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<IndenizacaoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar indenização vinculada a incidente", description = "Cria uma cobertura indenizatória associada a incidente operacional com valor, situação de pagamento, seguradora e protocolo para acompanhamento financeiro.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<IndenizacaoDTO>> create(@jakarta.validation.Valid @RequestBody CreateIndenizacaoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar indenização vinculada a incidente", description = "Mantém valor, situação de pagamento, seguradora, processo e vínculo com incidente para pagamento, conciliação financeira e auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<IndenizacaoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateIndenizacaoDTO dto) {
        return super.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover indenização do cadastro financeiro", description = "Remove uma indenização do cadastro administrativo quando a base financeira exige saneamento, revisão de sinistro ou retirada de registros obsoletos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover indenizações em lote", description = "Remove várias indenizações em uma única chamada para saneamento administrativo, revisão financeira ou limpeza controlada de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }
}









