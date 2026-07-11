package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.config.DomainRuleApprovalPolicyResolver;
import com.example.praxis.apiquickstart.core.service.ResourceActionTransitionService;
import com.example.praxis.apiquickstart.hr.dto.CreateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.EventosFolhaResponseDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateEventosFolhaDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.BulkApproveEventosFolhaResultDTO;
import com.example.praxis.apiquickstart.hr.dto.actions.RejectEventoFolhaRequestDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.EventosFolhaFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.EventosFolha;
import com.example.praxis.apiquickstart.hr.enums.StatusEventoFolha;
import com.example.praxis.apiquickstart.hr.mapper.EventosFolhaMapper;
import com.example.praxis.apiquickstart.hr.repository.EventosFolhaRepository;
import lombok.extern.slf4j.Slf4j;
import org.praxisplatform.uischema.exporting.CollectionExportCapability;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportExecutor;
import org.praxisplatform.uischema.exporting.CollectionExportFormat;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.exporting.CollectionExportResult;
import org.praxisplatform.uischema.exporting.CollectionExportScope;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.praxisplatform.uischema.service.base.AbstractBaseResourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Service de eventos de folha usado para demonstrar workflow action de colecao.
 *
 * <p>Ao contrario de um update comum em lote, este service explicita a semantica da acao
 * {@code bulk-approve}: validar estado anterior, tentar a transicao por item e devolver um
 * resultado detalhado por id. Isso torna o workflow legivel tanto para humanos quanto para clientes
 * que consomem o contrato publicado.</p>
 */
@Service
@Slf4j
public class EventosFolhaService extends AbstractBaseResourceService<
        EventosFolha,
        EventosFolhaResponseDTO,
        Integer,
        EventosFolhaFilterDTO,
        CreateEventosFolhaDTO,
        UpdateEventosFolhaDTO> {

    private static final String BULK_APPROVE_TARGET_ARTIFACT_KEY = "human-resources.eventos-folha:bulk-approve";
    private static final String REJECT_TARGET_ARTIFACT_KEY = "human-resources.eventos-folha:reject";
    private static final int EXPORT_MAX_ROWS = 500;
    private static final List<CollectionExportField> DEFAULT_EXPORT_FIELDS = List.of(
            new CollectionExportField("id", "ID", true, true, "number", "id"),
            new CollectionExportField("descricao", "Descricao", true, true, "string", "descricao"),
            new CollectionExportField("tipo", "Tipo", true, true, "string", "tipo"),
            new CollectionExportField("valor", "Valor", true, true, "number", "valor"),
            new CollectionExportField("folhaPagamentoId", "Folha Pagamento ID", true, true, "number", "folhaPagamentoId"),
            new CollectionExportField("folhaPagamentoNome", "Folha", true, true, "string", "folhaPagamentoNome")
    );
    private static final CollectionExportCapability EXPORT_CAPABILITY = new CollectionExportCapability(
            List.of(CollectionExportFormat.CSV, CollectionExportFormat.JSON),
            List.of(
                    CollectionExportScope.AUTO,
                    CollectionExportScope.SELECTED,
                    CollectionExportScope.FILTERED,
                    CollectionExportScope.CURRENT_PAGE,
                    CollectionExportScope.ALL
            ),
            Map.of(CollectionExportFormat.CSV.value(), EXPORT_MAX_ROWS, CollectionExportFormat.JSON.value(), EXPORT_MAX_ROWS),
            false
    );

    private final EventosFolhaMapper mapper;
    private final CollectionExportExecutor collectionExportExecutor;
    private final DomainRuleApprovalPolicyResolver approvalPolicyResolver;
    private final ResourceActionTransitionService transitionService;
    private final EventosFolhaApprovalItemService approvalItemService;

    public EventosFolhaService(
            EventosFolhaRepository repository,
            EventosFolhaMapper mapper,
            CollectionExportExecutor collectionExportExecutor,
            DomainRuleApprovalPolicyResolver approvalPolicyResolver,
            ResourceActionTransitionService transitionService,
            EventosFolhaApprovalItemService approvalItemService
    ) {
        super(repository, EventosFolha.class);
        this.mapper = mapper;
        this.collectionExportExecutor = collectionExportExecutor;
        this.approvalPolicyResolver = approvalPolicyResolver;
        this.transitionService = transitionService;
        this.approvalItemService = approvalItemService;
    }

    @Override
    protected ResourceMapper<EventosFolha, EventosFolhaResponseDTO, CreateEventosFolhaDTO, UpdateEventosFolhaDTO, Integer> getResourceMapper() {
        return mapper;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }

    @Override
    @Transactional(readOnly = true)
    public OptionalLong getResourceVersion(Integer id) {
        return getRepository().findById(id)
                .map(EventosFolha::getVersion)
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    @Override
    public boolean supportsCollectionExport() {
        return true;
    }

    @Override
    public Optional<CollectionExportCapability> getCollectionExportCapability() {
        return Optional.of(EXPORT_CAPABILITY);
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionExportResult exportCollection(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        CollectionExportRequest<EventosFolhaFilterDTO> effectiveRequest = normalizeExportRequest(request);
        ExportRows exportRows = resolveExportRows(effectiveRequest);
        CollectionExportResult result = collectionExportExecutor.export(
                effectiveRequest,
                exportRows.rows(),
                DEFAULT_EXPORT_FIELDS,
                this::valueFor,
                exportMetadata(effectiveRequest, exportRows)
        );
        return exportRows.needsWarning()
                ? withExportWarning(result, exportRows)
                : result;
    }

    public BulkApproveEventosFolhaResultDTO bulkApprove(
            BulkApproveEventosFolhaRequestDTO request,
            String actorSubject,
            String correlationId
    ) {
        enforceApprovalPolicy(BULK_APPROVE_TARGET_ARTIFACT_KEY);
        java.util.List<Integer> ids = request == null || request.getIds() == null
                ? java.util.List.of()
                : request.getIds().stream()
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        return changeStatusInBulk(ids, StatusEventoFolha.PENDENTE, StatusEventoFolha.APROVADO, request, actorSubject, correlationId);
    }

    @Transactional
    public UUID reject(Integer id, RejectEventoFolhaRequestDTO command, String actorSubject, String correlationId) {
        enforceApprovalPolicy(REJECT_TARGET_ARTIFACT_KEY);
        var replay = transitionService.findReplay("human-resources.eventos-folha", id, "reject");
        if (replay.isPresent()) return replay.get();
        EventosFolha event = getRepository().findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll event not found."));
        if (event.getStatus() != StatusEventoFolha.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payroll event is not pending.");
        }
        long versionBefore = event.getVersion() == null ? 0L : event.getVersion();
        event.setStatus(StatusEventoFolha.REJEITADO);
        EventosFolha saved = getRepository().saveAndFlush(event);
        return transitionService.record("human-resources.eventos-folha", id, "reject", "ITEM",
                StatusEventoFolha.PENDENTE.name(), StatusEventoFolha.REJEITADO.name(), command.getReasonCode(),
                command.getComment(), command.getEffectiveAt(), actorSubject, correlationId, versionBefore, saved.getVersion());
    }

    public Optional<UUID> findRejectReplay(Integer id) {
        return transitionService.findReplay("human-resources.eventos-folha", id, "reject");
    }

    private void enforceApprovalPolicy(String targetArtifactKey) {
        approvalPolicyResolver.resolveAppliedPolicy(targetArtifactKey)
                .ifPresent(policy -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, policy.effectiveMessage());
                });
    }

    /** Executa a transicao item a item para manter feedback preciso do lote processado. */
    private BulkApproveEventosFolhaResultDTO changeStatusInBulk(
            java.util.List<Integer> ids,
            StatusEventoFolha requiredStatus,
            StatusEventoFolha targetStatus,
            BulkApproveEventosFolhaRequestDTO command,
            String actorSubject,
            String correlationId
    ) {
        BulkApproveEventosFolhaResultDTO result = new BulkApproveEventosFolhaResultDTO();
        if (ids == null || ids.isEmpty()) {
            result.setTotal(0);
            result.setProcessed(0);
            result.setFailed(0);
            return result;
        }

        result.setTotal(ids.size());
        int processed = 0;
        for (Integer id : ids) {
            try {
                UUID transitionId = approvalItemService.approve(id, command, actorSubject, correlationId);
                result.getDetails().add(new BulkApproveEventosFolhaResultDTO.ItemResult(id, transitionId, true, null));
                processed++;
            } catch (Exception ex) {
                log.warn("Falha ao atualizar workflow de eventos_folha para id={}", id, ex);
                String error = ex instanceof ResponseStatusException response && response.getReason() != null
                        ? response.getReason()
                        : "Failed to update workflow status";
                result.getDetails().add(new BulkApproveEventosFolhaResultDTO.ItemResult(id, false, error));
            }
        }

        result.setProcessed(processed);
        result.setFailed(result.getTotal() - processed);
        return result;
    }

    private ExportRows resolveExportRows(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        CollectionExportScope scope = resolveScope(request);
        return switch (scope) {
            case SELECTED -> resolveSelectedRows(request);
            case CURRENT_PAGE -> {
                Page<EventosFolhaResponseDTO> page = filter(filtersOrEmpty(request), pageableFromRequest(request), null);
                yield new ExportRows(
                        page.getContent(),
                        page.getTotalElements(),
                        page.getTotalElements() > page.getContent().size(),
                        effectiveMaxRows(request),
                        requestedMaxRowsExceedsServerLimit(request)
                );
            }
            case FILTERED, ALL -> {
                Page<EventosFolhaResponseDTO> page = filter(filtersOrEmpty(request), maxRowsPageable(request), null);
                yield new ExportRows(
                        page.getContent(),
                        page.getTotalElements(),
                        page.getTotalElements() > page.getContent().size(),
                        effectiveMaxRows(request),
                        requestedMaxRowsExceedsServerLimit(request)
                );
            }
            case AUTO -> throw new IllegalStateException("AUTO scope should have been resolved before export.");
        };
    }

    private CollectionExportScope resolveScope(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        if (request.scope() != CollectionExportScope.AUTO) {
            return request.scope();
        }
        if (request.selection() != null
                && ((request.selection().selectedKeys() != null && !request.selection().selectedKeys().isEmpty())
                || Boolean.TRUE.equals(request.selection().allMatchingSelected()))) {
            return CollectionExportScope.SELECTED;
        }
        return request.filters() != null
                ? CollectionExportScope.FILTERED
                : CollectionExportScope.CURRENT_PAGE;
    }

    private ExportRows resolveSelectedRows(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        int maxRows = effectiveMaxRows(request);
        if (request.selection() != null && Boolean.TRUE.equals(request.selection().allMatchingSelected())) {
            Page<EventosFolhaResponseDTO> matching = filter(filtersOrEmpty(request), maxRowsPageable(request), null);
            List<Integer> excluded = toIntegerIds(request.selection().excludedKeys());
            List<EventosFolhaResponseDTO> rows = excluded.isEmpty()
                    ? matching.getContent()
                    : matching.getContent().stream()
                    .filter(row -> !excluded.contains(row.getId()))
                    .toList();
            return new ExportRows(
                    rows,
                    matching.getTotalElements(),
                    matching.getTotalElements() > rows.size(),
                    maxRows,
                    requestedMaxRowsExceedsServerLimit(request)
            );
        }

        List<Integer> selectedIds = request.selection() == null
                ? List.of()
                : toIntegerIds(request.selection().selectedKeys());
        if (selectedIds.isEmpty()) {
            return new ExportRows(List.of(), 0, false, maxRows, requestedMaxRowsExceedsServerLimit(request));
        }
        List<Integer> limitedIds = selectedIds.size() > maxRows ? selectedIds.subList(0, maxRows) : selectedIds;
        return new ExportRows(
                findAllById(limitedIds),
                selectedIds.size(),
                selectedIds.size() > maxRows,
                maxRows,
                requestedMaxRowsExceedsServerLimit(request)
        );
    }

    private Pageable pageableFromRequest(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        int page = request.pagination() != null && request.pagination().pageIndex() != null
                ? request.pagination().pageIndex()
                : request.pagination() != null && request.pagination().pageNumber() != null
                ? request.pagination().pageNumber()
                : 0;
        int size = request.pagination() != null && request.pagination().pageSize() != null
                ? request.pagination().pageSize()
                : effectiveMaxRows(request);
        return PageRequest.of(Math.max(page, 0), Math.max(Math.min(size, effectiveMaxRows(request)), 1), sortFromRequest(request));
    }

    private Pageable maxRowsPageable(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        return PageRequest.of(0, effectiveMaxRows(request), sortFromRequest(request));
    }

    private Sort sortFromRequest(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        Sort requestedSort = parseSort(request.sort());
        return requestedSort.isSorted() ? requestedSort : getDefaultSort();
    }

    private Sort parseSort(Object sort) {
        if (sort instanceof List<?> entries) {
            List<Sort.Order> orders = entries.stream()
                    .map(this::parseSortOrder)
                    .flatMap(Optional::stream)
                    .toList();
            return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
        }
        return parseSortOrder(sort)
                .map(Sort::by)
                .orElse(Sort.unsorted());
    }

    private Optional<Sort.Order> parseSortOrder(Object value) {
        if (value instanceof Map<?, ?> map) {
            String field = firstText(map.get("field"), map.get("property"), map.get("active"), map.get("key"));
            String direction = firstText(map.get("direction"), map.get("dir"));
            return sortOrder(field, direction);
        }
        if (value instanceof String text && !text.isBlank()) {
            String[] parts = text.split(",", 2);
            return sortOrder(parts[0], parts.length > 1 ? parts[1] : "asc");
        }
        return Optional.empty();
    }

    private Optional<Sort.Order> sortOrder(String requestedField, String requestedDirection) {
        String property = exportSortProperty(requestedField);
        if (property == null) {
            return Optional.empty();
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(requestedDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Optional.of(new Sort.Order(direction, property));
    }

    private String exportSortProperty(String requestedField) {
        if (requestedField == null || requestedField.isBlank()) {
            return null;
        }
        return switch (requestedField.trim()) {
            case "id" -> "id";
            case "descricao" -> "descricao";
            case "tipo" -> "tipo";
            case "valor" -> "valor";
            case "folhaPagamentoId" -> "folhaPagamento.id";
            case "folhaPagamentoNome" -> "folhaPagamento.nome";
            default -> null;
        };
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private EventosFolhaFilterDTO filtersOrEmpty(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        return request.filters() != null ? request.filters() : new EventosFolhaFilterDTO();
    }

    private int effectiveMaxRows(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        int requested = request.maxRows() != null && request.maxRows() > 0 ? request.maxRows() : EXPORT_MAX_ROWS;
        return Math.min(requested, EXPORT_MAX_ROWS);
    }

    private boolean requestedMaxRowsExceedsServerLimit(CollectionExportRequest<EventosFolhaFilterDTO> request) {
        return request.maxRows() != null && request.maxRows() > EXPORT_MAX_ROWS;
    }

    private List<Integer> toIntegerIds(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString()))
                .distinct()
                .toList();
    }

    private Object valueFor(EventosFolhaResponseDTO row, CollectionExportField field) {
        String key = field.valuePath() != null && !field.valuePath().isBlank()
                ? field.valuePath()
                : field.key();
        return switch (key) {
            case "id" -> row.getId();
            case "descricao" -> row.getDescricao();
            case "tipo" -> row.getTipo();
            case "valor" -> row.getValor();
            case "folhaPagamentoId" -> row.getFolhaPagamentoId();
            case "folhaPagamentoNome" -> row.getFolhaPagamentoNome();
            default -> "";
        };
    }

    private CollectionExportRequest<EventosFolhaFilterDTO> normalizeExportRequest(
            CollectionExportRequest<EventosFolhaFilterDTO> request
    ) {
        CollectionExportRequest<EventosFolhaFilterDTO> source = request == null
                ? new CollectionExportRequest<>(
                        "table",
                        null,
                        null,
                        CollectionExportFormat.CSV,
                        CollectionExportScope.ALL,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        Map.of(),
                        true,
                        false,
                        null,
                        null,
                        Map.of()
                )
                : request;
        CollectionExportFormat format = source.format() == null ? CollectionExportFormat.CSV : source.format();
        CollectionExportScope scope = source.scope() == null ? CollectionExportScope.ALL : source.scope();
        String fileName = source.fileName() != null && !source.fileName().isBlank()
                ? source.fileName()
                : "eventos-folha." + extensionFor(format);
        return new CollectionExportRequest<>(
                source.componentType(),
                source.componentId(),
                source.resourcePath(),
                format,
                scope,
                source.selection(),
                source.fields(),
                source.filters(),
                source.sort(),
                source.pagination(),
                source.query(),
                source.includeHeaders(),
                source.applyFormatting(),
                effectiveMaxRows(source),
                fileName,
                source.formatOptions(),
                source.localization(),
                source.metadata()
        );
    }

    private String extensionFor(CollectionExportFormat format) {
        return switch (format) {
            case JSON -> "json";
            case CSV -> "csv";
            case EXCEL -> "xlsx";
            case PDF -> "pdf";
            case PRINT -> "html";
        };
    }

    private Map<String, Object> exportMetadata(
            CollectionExportRequest<EventosFolhaFilterDTO> request,
            ExportRows exportRows
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resourceKey", "human-resources.eventos-folha");
        metadata.put("maxRows", exportRows.maxRows());
        metadata.put("candidateRows", exportRows.candidateRows());
        metadata.put("truncated", exportRows.truncated());
        metadata.put("requestedMaxRowsExceeded", exportRows.requestedMaxRowsExceeded());
        metadata.put("scope", request.scope().value());
        return metadata;
    }

    private CollectionExportResult withExportWarning(CollectionExportResult result, ExportRows exportRows) {
        List<String> warnings = new ArrayList<>(result.warnings());
        if (exportRows.requestedMaxRowsExceeded()) {
            warnings.add("Requested maxRows exceeds the server limit of " + EXPORT_MAX_ROWS + "; the server limit was applied.");
        }
        if (exportRows.truncated()) {
            warnings.add("Export truncated to " + exportRows.rows().size() + " rows from " + exportRows.candidateRows() + " matching rows.");
        }
        return new CollectionExportResult(
                result.status(),
                result.format(),
                result.scope(),
                result.content(),
                result.fileName(),
                result.contentType(),
                result.downloadUrl(),
                result.jobId(),
                result.rowCount(),
                warnings,
                result.metadata()
        );
    }

    private record ExportRows(
            List<EventosFolhaResponseDTO> rows,
            long candidateRows,
            boolean truncated,
            int maxRows,
            boolean requestedMaxRowsExceeded
    ) {

        boolean needsWarning() {
            return truncated || requestedMaxRowsExceeded;
        }
    }
}
