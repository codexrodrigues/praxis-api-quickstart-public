package com.example.praxis.apiquickstart.operations.controller;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.dto.CreateMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.MissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.MissaoEventoDTO;
import com.example.praxis.apiquickstart.operations.dto.MissaoParticipanteDTO;
import com.example.praxis.apiquickstart.operations.dto.PlanejarEquipeMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.RescheduleMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.UpdateMissaoDTO;
import com.example.praxis.apiquickstart.operations.dto.VwResumoMissoeDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.MissaoWorkflowRequestDTO;
import com.example.praxis.apiquickstart.operations.dto.actions.MissaoWorkflowResultDTO;
import com.example.praxis.apiquickstart.operations.dto.filter.MissaoFilterDTO;
import com.example.praxis.apiquickstart.core.controller.base.AbstractQuickstartCrudController;
import com.example.praxis.apiquickstart.operations.entity.Missao;
import com.example.praxis.apiquickstart.operations.mapper.MissaoMapper;
import com.example.praxis.apiquickstart.operations.service.MissaoEventoService;
import com.example.praxis.apiquickstart.operations.service.MissaoParticipanteService;
import com.example.praxis.apiquickstart.operations.service.MissaoService;
import com.example.praxis.apiquickstart.operations.service.VwResumoMissoeService;
import org.praxisplatform.uischema.annotation.ApiGroup;
import org.praxisplatform.uischema.annotation.ApiResource;
import org.praxisplatform.uischema.annotation.ResourceIntent;
import org.praxisplatform.uischema.annotation.UiSurface;
import org.praxisplatform.uischema.annotation.WorkflowAction;
import org.praxisplatform.uischema.action.ActionScope;
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
 * Recurso de missoes usado como principal exemplo operacional de workflow contextual.
 *
 * <p>Este controller mostra, fora do dominio de RH, como a plataforma modela um recurso que
 * combina CRUD, surface parcial e varias workflow actions de item orientadas por estado. Ele e uma
 * referencia importante para qualquer caso em que a disponibilidade de acoes dependa do ciclo de
 * vida real do agregado.</p>
 */
@ApiResource(value = ApiPaths.Operations.MISSOES, resourceKey = "operations.missoes")
@ApiGroup("operations")
public class MissaoController extends AbstractQuickstartCrudController<Missao, MissaoDTO, Integer, MissaoFilterDTO, CreateMissaoDTO, UpdateMissaoDTO> {

    private final MissaoService service;
    private final MissaoMapper mapper;
    private final VwResumoMissoeService resumoMissoeService;
    private final MissaoParticipanteService participanteService;
    private final MissaoEventoService eventoService;

    @Autowired
    public MissaoController(
            MissaoService service,
            MissaoMapper mapper,
            VwResumoMissoeService resumoMissoeService,
            MissaoParticipanteService participanteService,
            MissaoEventoService eventoService
    ) {
        this.service = service;
        this.mapper = mapper;
        this.resumoMissoeService = resumoMissoeService;
        this.participanteService = participanteService;
        this.eventoService = eventoService;
    }

    @Override
    protected MissaoService getService() { return service; }

    @Override
    protected MissaoDTO toDto(Missao entity) { return mapper.toDto(entity); }

    @Override
    protected Missao toEntity(MissaoDTO dto) { return mapper.toEntity(dto); }

    @Override
    protected Integer getEntityId(Missao entity) { return entity.getId(); }

    @Override
    protected Integer getDtoId(MissaoDTO dto) { return dto.getId(); }

    @PostMapping("/filter")
    @Operation(summary = "Filtrar missões de operação", description = "Lista missões por objetivo, equipe responsável, prioridade, status, local e janela de execução para planejamento e acompanhamento operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.springframework.data.domain.Page<EntityModel<MissaoDTO>>>> filter(@RequestBody MissaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam(name = "includeIds", required = false) List<Integer> includeIds, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filter(filterDTO, page, size, includeIds, queryParams);
    }

    @PostMapping("/filter/cursor")
    @Operation(summary = "Filtrar missões de operação com paginação por cursor", description = "Percorre missões em grandes catálogos usando cursor, útil para centros de operação, listas extensas e navegação incremental.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<RestApiResponse<org.praxisplatform.uischema.dto.CursorPage<EntityModel<MissaoDTO>>>> filterByCursor(@RequestBody MissaoFilterDTO filterDTO, @RequestParam(name = "after", required = false) String after, @RequestParam(name = "before", required = false) String before, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterByCursor(filterDTO, after, before, size, queryParams);
    }

    @PostMapping("/locate")
    @Operation(summary = "Localizar missão de operação em listas filtradas", description = "Informa em qual posição uma missão aparece dentro do recorte filtrado, útil para retomar a análise do item em tabelas operacionais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posição localizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.praxisplatform.uischema.dto.LocateResponse> locate(@RequestBody MissaoFilterDTO filterDTO, @RequestParam("id") Integer id, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.locate(filterDTO, id, size, queryParams);
    }

    @GetMapping("/all")
    @Operation(summary = "Consultar missões de operação", description = "Retorna o cadastro completo de missões quando o consumidor precisa materializar toda a carteira operacional para exportação, sincronização ou conferência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<java.util.List<EntityModel<MissaoDTO>>>> getAll() {
        return super.getAll();
    }

    @GetMapping("/by-ids")
    @Operation(summary = "Buscar missões de operação por IDs", description = "Recupera missões já referenciadas por eventos, participantes ou seleções anteriores sem reaplicar filtros de planejamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros retornados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<MissaoDTO>> getByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getByIds(ids);
    }

    @PostMapping("/options/filter")
    @Operation(summary = "Selecionar missões de operação para formulários", description = "Produz opções compactas de missões para campos de seleção, autocomplete e vínculos operacionais orientados por busca.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou filtro inconsistente.")
    })
    public ResponseEntity<org.springframework.data.domain.Page<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> filterOptions(@RequestBody MissaoFilterDTO filterDTO, @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "20") int size, @RequestParam MultiValueMap<String, String> queryParams) {
        return super.filterOptions(filterDTO, page, size, queryParams);
    }

    @GetMapping("/options/by-ids")
    @Operation(summary = "Carregar missões de operação selecionadas", description = "Reidrata opções de missões já escolhidas em formulários e filtros salvos, preservando identificador e rótulo exibido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções retornadas com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<List<org.praxisplatform.uischema.dto.OptionDTO<Integer>>> getOptionsByIds(@RequestParam(name = "ids", required = false) List<Integer> ids) {
        return super.getOptionsByIds(ids);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter missão de operação", description = "Retorna o detalhe de uma missão para inspeção operacional, auditoria de planejamento ou composição de surfaces dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<RestApiResponse<MissaoDTO>> getById(@PathVariable Integer id) {
        return super.getById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastrar missão de operação", description = "Cadastra uma nova missão com objetivo, prioridade, equipe e janela prevista para execução operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MissaoDTO>> create(@jakarta.validation.Valid @RequestBody CreateMissaoDTO dto) {
        return super.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar missão de operação", description = "Atualiza dados de uma missão existente sem alterar sua identidade, preservando coerência para eventos e participantes dependentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflito de dados ou violação de regra de negócio.")
    })
    public ResponseEntity<RestApiResponse<MissaoDTO>> update(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody UpdateMissaoDTO dto) {
        return super.update(id, dto);
    }

    @PatchMapping("/{id}/reschedule")
    @UiSurface(
            id = "reschedule",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Reschedule mission",
            description = "Adjust schedule and execution context without changing the mission lifecycle",
            intent = "mission-reschedule",
            order = 30,
            allowedStates = {"PLANEJADA", "PAUSADA"},
            tags = {"planning", "schedule"}
    )
    @ResourceIntent(
            id = "mission-reschedule",
            title = "Reschedule mission",
            description = "Adjust timing and location while preserving mission history",
            order = 30
    )
    @Operation(summary = "Reagendar missão", description = "Reagenda local e janela prevista da missão sem alterar o histórico nem recriar o recurso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Missão atualizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou dados inconsistentes."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado."),
            @ApiResponse(responseCode = "409", description = "Estado atual não permite reagendamento.")
    })
    public ResponseEntity<RestApiResponse<MissaoDTO>> reschedule(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody RescheduleMissaoDTO dto
    ) {
        MissaoDTO updated = service.reschedule(id, dto);
        // Mantem a surface parcial autodocumentada e reentravel pelo schema publicado no host.
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/reschedule", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(updated, hateoasOrNull(links)));
    }

    @PatchMapping("/{id}/team-plan")
    @UiSurface(
            id = "team-plan",
            kind = SurfaceKind.PARTIAL_FORM,
            scope = SurfaceScope.ITEM,
            title = "Planejar equipe",
            description = "Edita a colecao planejada de participantes da missao",
            intent = "mission-team-planning",
            order = 35,
            allowedStates = {"PLANEJADA", "PAUSADA"},
            tags = {"mission", "team", "editable-collection"}
    )
    @ResourceIntent(
            id = "mission-team-planning",
            title = "Planejamento de equipe",
            description = "Configura participantes, papeis e responsavel principal da missao",
            order = 35
    )
    @Operation(summary = "Planejar equipe da missao", description = "Publica um formulario parcial com colecao editavel de participantes para validar o contrato canonico x-ui.array.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planejamento recebido com sucesso."),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida ou colecao inconsistente."),
            @ApiResponse(responseCode = "404", description = "Registro nao encontrado."),
            @ApiResponse(responseCode = "409", description = "Estado atual nao permite planejamento de equipe.")
    })
    public ResponseEntity<RestApiResponse<MissaoDTO>> planTeam(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody PlanejarEquipeMissaoDTO dto
    ) {
        MissaoDTO updated = service.planTeam(id, dto);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToFilterCursor(),
                linkToUiSchema("/{id}/team-plan", "patch", "request")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(updated, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/summary")
    @UiSurface(
            id = "summary",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Resumo operacional",
            description = "Consolida status, prioridade, ameaça, participantes e eventos da missão",
            intent = "mission-command-center",
            order = 40,
            tags = {"mission", "summary", "read-projection"}
    )
    @ResourceIntent(
            id = "mission-command-center",
            title = "Centro de comando da missão",
            description = "Mostra uma leitura agregada da missão para acompanhamento operacional",
            order = 40
    )
    @Operation(summary = "Obter resumo operacional da missão", description = "Retorna a visão agregada da missão com contadores de participantes, eventos e contexto de ameaça.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Resumo não encontrado.")
    })
    public ResponseEntity<RestApiResponse<VwResumoMissoeDTO>> getSummary(@PathVariable Integer id) {
        VwResumoMissoeDTO summary = resumoMissoeService.findById(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/summary", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(summary, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/team")
    @UiSurface(
            id = "team",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Equipe da missão",
            description = "Lista participantes, papéis e resultados associados à missão",
            intent = "mission-command-center",
            order = 50,
            tags = {"mission", "team", "read-projection"}
    )
    @Operation(summary = "Obter equipe da missão", description = "Retorna os participantes vinculados à missão para compor superfícies de comando operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipe retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<MissaoParticipanteDTO>>> getTeam(@PathVariable Integer id) {
        List<MissaoParticipanteDTO> team = participanteService.findByMissaoIdForCommandCenter(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/team", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(team, hateoasOrNull(links)));
    }

    @GetMapping("/{id}/timeline")
    @UiSurface(
            id = "timeline",
            kind = SurfaceKind.READ_PROJECTION,
            scope = SurfaceScope.ITEM,
            title = "Linha do tempo",
            description = "Lista eventos recentes da missão em ordem cronológica operacional",
            intent = "mission-command-center",
            order = 60,
            tags = {"mission", "timeline", "read-projection"}
    )
    @Operation(summary = "Obter linha do tempo da missão", description = "Retorna os eventos recentes da missão para leitura temporal e auditoria operacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linha do tempo retornada com sucesso.")
    })
    public ResponseEntity<RestApiResponse<List<MissaoEventoDTO>>> getTimeline(@PathVariable Integer id) {
        List<MissaoEventoDTO> timeline = eventoService.findTop20ByMissaoIdForTimeline(id);
        Links links = Links.of(
                linkToSelf(id),
                linkToAll(),
                linkToFilter(),
                linkToUiSchema("/{id}/timeline", "get", "response")
        );
        return withVersion(ResponseEntity.ok(), RestApiResponse.success(timeline, hateoasOrNull(links)));
    }

    @PostMapping("/{id}/actions/start")
    @Operation(summary = "Iniciar missão", description = "Inicia a execução da missão e publica a transição de estado para catálogos de workflow e capabilities.")
    @WorkflowAction(
            id = "start",
            title = "Iniciar missao",
            description = "Coloca uma missao planejada em execucao ativa e registra o marco inicial da operacao.",
            scope = ActionScope.ITEM,
            order = 100,
            successMessage = "Missao iniciada",
            allowedStates = {"PLANEJADA"},
            tags = {"workflow", "status"}
    )
    public ResponseEntity<RestApiResponse<MissaoWorkflowResultDTO>> start(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody MissaoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/start", service.start(id, dto));
    }

    @PostMapping("/{id}/actions/pause")
    @Operation(summary = "Pausar missão", description = "Pausa uma missão em andamento mantendo o contexto operacional necessário para retomada.")
    @WorkflowAction(
            id = "pause",
            title = "Pausar missao",
            description = "Interrompe temporariamente uma missao em andamento preservando contexto para retomada.",
            scope = ActionScope.ITEM,
            order = 110,
            successMessage = "Missao pausada",
            allowedStates = {"EM_ANDAMENTO"},
            tags = {"workflow", "status"}
    )
    public ResponseEntity<RestApiResponse<MissaoWorkflowResultDTO>> pause(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody MissaoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/pause", service.pause(id, dto));
    }

    @PostMapping("/{id}/actions/resume")
    @Operation(summary = "Retomar missão", description = "Retoma uma missão pausada e republica sua disponibilidade para ações subsequentes.")
    @WorkflowAction(
            id = "resume",
            title = "Retomar missao",
            description = "Retoma uma missao pausada e recoloca a operacao no fluxo ativo de acompanhamento.",
            scope = ActionScope.ITEM,
            order = 120,
            successMessage = "Missao retomada",
            allowedStates = {"PAUSADA"},
            tags = {"workflow", "status"}
    )
    public ResponseEntity<RestApiResponse<MissaoWorkflowResultDTO>> resume(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody MissaoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/resume", service.resume(id, dto));
    }

    @PostMapping("/{id}/actions/complete")
    @Operation(summary = "Concluir missão", description = "Conclui a missão e fecha o ciclo operacional com atualização de estado e data final.")
    @WorkflowAction(
            id = "complete",
            title = "Concluir missao",
            description = "Encerra uma missao executada com sucesso e fixa seu fechamento operacional.",
            scope = ActionScope.ITEM,
            order = 130,
            successMessage = "Missao concluida",
            allowedStates = {"EM_ANDAMENTO"},
            tags = {"workflow", "status"}
    )
    public ResponseEntity<RestApiResponse<MissaoWorkflowResultDTO>> complete(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody MissaoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/complete", service.complete(id, dto));
    }

    @PostMapping("/{id}/actions/fail")
    @Operation(summary = "Registrar falha da missão", description = "Encerra a missão com falha e publica o resultado operacional para surfaces, actions e auditoria.")
    @WorkflowAction(
            id = "fail",
            title = "Registrar falha da missao",
            description = "Encerra uma missao com falha operacional e preserva o contexto para auditoria e acompanhamento.",
            scope = ActionScope.ITEM,
            order = 140,
            successMessage = "Falha da missao registrada",
            allowedStates = {"EM_ANDAMENTO", "PAUSADA"},
            tags = {"workflow", "status"}
    )
    public ResponseEntity<RestApiResponse<MissaoWorkflowResultDTO>> fail(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody MissaoWorkflowRequestDTO dto
    ) {
        return workflowResponse(id, "/{id}/actions/fail", service.fail(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover missão de operação", description = "Exclui uma missão quando ela não deve mais compor o catálogo operacional nem ser reutilizada em novos fluxos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido com sucesso."),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        return super.delete(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Remover missões de operação em lote", description = "Exclui múltiplas missões em uma única chamada para saneamento administrativo ou limpeza de dados de demonstração.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registros removidos com sucesso."),
            @ApiResponse(responseCode = "400", description = "Lista de IDs inválida.")
    })
    public ResponseEntity<Void> deleteBatch(@RequestBody List<Integer> ids) {
        return super.deleteBatch(ids);
    }

    private ResponseEntity<RestApiResponse<MissaoWorkflowResultDTO>> workflowResponse(
            Integer id,
            String operationPath,
            MissaoWorkflowResultDTO result
    ) {
        // Cada action reapresenta os links do recurso e dos schemas da transicao executada.
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











