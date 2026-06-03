package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicy;
import com.example.praxis.apiquickstart.config.DomainRuleWorkflowActionPolicyResolver;
import com.example.praxis.apiquickstart.hr.dto.CreateFolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.FolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.ScheduleFolhaPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFolhasPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.FolhaPagamentoWorkflowRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.FolhaPagamentoWorkflowResultDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FolhasPagamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.FolhasPagamento;
import com.example.praxis.apiquickstart.hr.mapper.FolhasPagamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.FolhasPagamentoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.praxisplatform.uischema.capability.ResourceStateSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service de folhas de pagamento usado para demonstrar workflow de item orientado por estado.
 *
 * <p>Este service concentra a parte mais pedagogica do recurso de folha no quickstart:</p>
 *
 * <ul>
 *   <li>calculo do estado semantico publicado para discovery contextual;</li>
 *   <li>surface parcial para reagendamento sem reabrir o agregado completo;</li>
 *   <li>workflow actions de item que validam transicoes de estado reais;</li>
 *   <li>adaptacao temporaria a ambientes onde a migration de status ainda nao foi aplicada.</li>
 * </ul>
 *
 * <p>Ele ajuda a mostrar que actions, capabilities e availability nao nascem de convencoes do
 * frontend: elas dependem de regra de negocio explicita no host.</p>
 */
@Service
public class FolhasPagamentoService extends AbstractQuickstartCrudService<FolhasPagamento, FolhasPagamentoDTO, Integer, FolhasPagamentoFilterDTO, CreateFolhasPagamentoDTO, UpdateFolhasPagamentoDTO> {

    private static final String STATE_AWAITING_EVENTS = "AGUARDANDO_EVENTOS";
    private static final String STATE_SCHEDULED = "PROGRAMADA";
    private static final String STATE_PAID = "PAGA";
    private static final String MARK_PAID_POLICY_TARGET = "human-resources.folhas-pagamento:mark-paid";
    private static final String STATUS_COLUMN_EXISTS_SQL = """
            select exists (
                select 1
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'eventos_folha'
                  and column_name = 'status'
            )
            """;
    private static final String COUNT_PENDING_EVENTS_SQL = """
            select count(*)
              from public.eventos_folha
             where folha_pagamento_id = ?
               and status = 'PENDENTE'
            """;
    private static final String APPROVE_PENDING_EVENTS_SQL = """
            update public.eventos_folha
               set status = 'APROVADO'
             where folha_pagamento_id = ?
               and status = 'PENDENTE'
            """;
    private static final String EVENT_STATUS_MIGRATION_REQUIRED_MESSAGE = "Workflow requires public.eventos_folha.status column";

    private final FolhasPagamentoRepository repository;
    private final FolhasPagamentoMapper mapper;
    private final JdbcTemplate apiJdbcTemplate;
    private final DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver;
    private volatile Boolean eventStatusColumnAvailable;

    public FolhasPagamentoService(
            FolhasPagamentoRepository repository,
            FolhasPagamentoMapper mapper,
            @Qualifier("apiJdbcTemplate") JdbcTemplate apiJdbcTemplate,
            DomainRuleWorkflowActionPolicyResolver workflowActionPolicyResolver
    ) {
        super(repository, FolhasPagamento.class, mapper::toDto, mapper::toEntity, mapper::toEntity, FolhasPagamento::getId);
        this.repository = repository;
        this.mapper = mapper;
        this.apiJdbcTemplate = apiJdbcTemplate;
        this.workflowActionPolicyResolver = workflowActionPolicyResolver;
    }

    @Override
    public FolhasPagamento mergeUpdate(FolhasPagamento existing, FolhasPagamento fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }

    public Optional<ResourceStateSnapshot> resolveStateSnapshot(Object resourceId) {
        Integer id = coerceInteger(resourceId);
        if (id == null) {
            return Optional.empty();
        }
        return loadSemanticSnapshot(id).map(snapshot -> ResourceStateSnapshot.of(resolveSemanticState(snapshot)));
    }

    /** Reagenda a folha sem alterar o restante do agregado financeiro. */
    @Transactional
    public FolhasPagamentoDTO schedulePayment(Integer id, ScheduleFolhaPagamentoDTO dto) {
        FolhaPagamentoSemanticSnapshot snapshot = loadSemanticSnapshot(id).orElseThrow(this::getNotFoundException);
        String currentState = resolveSemanticState(snapshot);
        if (STATE_PAID.equals(currentState)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + currentState);
        }
        LocalDate scheduledDate = dto.getDataPagamento();
        if (scheduledDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataPagamento must be greater than or equal to today");
        }
        FolhasPagamento existing = findEntityById(id);
        existing.setDataPagamento(scheduledDate);
        FolhasPagamento saved = refreshManaged(repository.save(existing));
        return mapper.toDto(saved);
    }

    /** Aprova eventos pendentes e avanca a folha do estado semantico para a proxima etapa. */
    @Transactional
    public FolhaPagamentoWorkflowResultDTO approvePendingEvents(Integer id, FolhaPagamentoWorkflowRequestDTO dto) {
        if (!hasEventStatusColumn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, EVENT_STATUS_MIGRATION_REQUIRED_MESSAGE);
        }
        FolhaPagamentoSemanticSnapshot before = loadSemanticSnapshot(id).orElseThrow(this::getNotFoundException);
        String previousState = resolveSemanticState(before);
        if (!STATE_AWAITING_EVENTS.equals(previousState)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + previousState);
        }
        int updated = apiJdbcTemplate.update(APPROVE_PENDING_EVENTS_SQL, id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + previousState);
        }
        FolhaPagamentoSemanticSnapshot after = loadSemanticSnapshot(id).orElseThrow(this::getNotFoundException);
        return buildWorkflowResult(
                id,
                previousState,
                resolveSemanticState(after),
                dto,
                updated,
                after.pendingEvents(),
                after.paymentDate(),
                "Pending payroll events approved"
        );
    }

    /** Marca a folha como paga apenas quando o workflow ja chegou ao estado programado. */
    @Transactional
    public FolhaPagamentoWorkflowResultDTO markAsPaid(Integer id, FolhaPagamentoWorkflowRequestDTO dto) {
        FolhaPagamentoSemanticSnapshot before = loadSemanticSnapshot(id).orElseThrow(this::getNotFoundException);
        String previousState = resolveSemanticState(before);
        if (!STATE_SCHEDULED.equals(previousState)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + previousState);
        }
        enforceWorkflowActionPolicy(previousState);
        LocalDate paidAt = LocalDate.now();
        int updated = repository.markAsPaid(id, paidAt);
        if (updated == 0) {
            FolhaPagamentoSemanticSnapshot currentSnapshot = loadSemanticSnapshot(id).orElseThrow(this::getNotFoundException);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "State not allowed: " + resolveSemanticState(currentSnapshot));
        }
        FolhaPagamentoSemanticSnapshot after = loadSemanticSnapshot(id).orElseThrow(this::getNotFoundException);
        return buildWorkflowResult(
                id,
                previousState,
                resolveSemanticState(after),
                dto,
                0,
                after.pendingEvents(),
                after.paymentDate(),
                "Payroll marked as paid"
        );
    }

    private void enforceWorkflowActionPolicy(String currentState) {
        workflowActionPolicyResolver.resolveAppliedPolicy(MARK_PAID_POLICY_TARGET)
                .filter(policy -> policy.appliesToState(currentState))
                .map(DomainRuleWorkflowActionPolicy::message)
                .filter(StringUtils::hasText)
                .ifPresent(message -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, message);
                });
    }

    /** Carrega o snapshot minimo necessario para calcular o estado semantico da folha. */
    private Optional<FolhaPagamentoSemanticSnapshot> loadSemanticSnapshot(Integer id) {
        return repository.findPaymentDateById(id)
                .map(paymentDate -> new FolhaPagamentoSemanticSnapshot(paymentDate, loadPendingEvents(id)));
    }

    private long loadPendingEvents(Integer id) {
        if (!hasEventStatusColumn()) {
            return 0L;
        }
        Long count = apiJdbcTemplate.queryForObject(COUNT_PENDING_EVENTS_SQL, Long.class, id);
        return count != null ? count : 0L;
    }

    private boolean hasEventStatusColumn() {
        Boolean cached = eventStatusColumnAvailable;
        if (Boolean.TRUE.equals(cached)) {
            return true;
        }
        Boolean exists = apiJdbcTemplate.queryForObject(STATUS_COLUMN_EXISTS_SQL, Boolean.class);
        boolean present = Boolean.TRUE.equals(exists);
        eventStatusColumnAvailable = present ? Boolean.TRUE : null;
        return present;
    }

    /** Traduz dados persistidos em um estado canonico consumido por surfaces/actions/capabilities. */
    private String resolveSemanticState(FolhaPagamentoSemanticSnapshot snapshot) {
        if (snapshot.pendingEvents() > 0) {
            return STATE_AWAITING_EVENTS;
        }
        if (snapshot.paymentDate().isAfter(LocalDate.now())) {
            return STATE_SCHEDULED;
        }
        return STATE_PAID;
    }

    private FolhaPagamentoWorkflowResultDTO buildWorkflowResult(
            Integer id,
            String previousState,
            String currentState,
            FolhaPagamentoWorkflowRequestDTO dto,
            int processedEvents,
            long pendingEvents,
            LocalDate paymentDate,
            String message
    ) {
        FolhaPagamentoWorkflowResultDTO result = new FolhaPagamentoWorkflowResultDTO();
        result.setId(id);
        result.setEstadoAnterior(previousState);
        result.setEstadoAtual(currentState);
        result.setJustificativa(dto != null ? dto.getJustificativa() : null);
        result.setEventosProcessados(processedEvents);
        result.setEventosPendentes(pendingEvents);
        result.setDataPagamento(paymentDate);
        result.setMensagem(message);
        return result;
    }

    private FolhasPagamento refreshManaged(FolhasPagamento entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        FolhasPagamento managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }

    private Integer coerceInteger(Object resourceId) {
        if (resourceId instanceof Integer integerId) {
            return integerId;
        }
        if (resourceId instanceof Number number) {
            return number.intValue();
        }
        if (resourceId instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** Snapshot interno minimo para decidir workflow e availability do recurso. */
    private record FolhaPagamentoSemanticSnapshot(LocalDate paymentDate, long pendingEvents) {
    }
}
