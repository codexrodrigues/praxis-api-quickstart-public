package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.FuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.CreateFuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioDTO;
import com.example.praxis.apiquickstart.hr.dto.UpdateFuncionarioProfileDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.FuncionarioFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import com.example.praxis.apiquickstart.hr.mapper.FuncionarioMapper;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioRepository;
import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import com.example.praxis.apiquickstart.core.service.ResourceActionTransitionService;
import com.example.praxis.apiquickstart.hr.dto.actions.FuncionarioDeactivateRequestDTO;
import org.praxisplatform.uischema.exporting.CollectionExportCapability;
import org.praxisplatform.uischema.exporting.CollectionExportExecutor;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFieldPresentation;
import org.praxisplatform.uischema.exporting.CollectionExportFormat;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.exporting.CollectionExportResult;
import org.praxisplatform.uischema.exporting.CollectionExportScope;
import org.praxisplatform.uischema.options.EntityLookupDescriptor;
import org.praxisplatform.uischema.options.LookupCapabilities;
import org.praxisplatform.uischema.options.LookupDetailDescriptor;
import org.praxisplatform.uischema.options.LookupDisplayDescriptor;
import org.praxisplatform.uischema.options.LookupDisplayFieldDescriptor;
import org.praxisplatform.uischema.options.LookupSelectionPolicy;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Service de funcionarios usado como referencia principal de regra de negocio CRUD no quickstart.
 *
 * <p>Este service mostra como um recurso comum da plataforma combina:</p>
 *
 * <ul>
 *   <li>mapeamento entre entidade e DTO pelo pipeline canonico do starter;</li>
 *   <li>merge controlado de update no agregado existente;</li>
 *   <li>surface parcial de perfil sem reusar cegamente o update completo;</li>
 *   <li>registro declarativo de campos de stats para discovery analitico.</li>
 * </ul>
 *
 * <p>Ele e uma boa referencia para quem quer entender como levar um CRUD tradicional para o modelo
 * metadata-driven da Praxis sem perder clareza de dominio.</p>
 */
@Service
public class FuncionarioService extends AbstractQuickstartCrudService<Funcionario, FuncionarioDTO, Integer, FuncionarioFilterDTO, CreateFuncionarioDTO, UpdateFuncionarioDTO> {
    public static final String EMPLOYEE_OPTION_SOURCE_KEY = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_SOURCE;

    @Override
    @Transactional(readOnly = true)
    public OptionalLong getResourceVersion(Integer id) {
        Long version = findEntityById(id).getVersion();
        return version == null ? OptionalLong.of(0L) : OptionalLong.of(version);
    }
    private static final int EXPORT_MAX_ROWS = 500;
    private static final Locale EXPORT_LOCALE = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", EXPORT_LOCALE);
    private static final List<CollectionExportField> DEFAULT_EXPORT_FIELDS = List.of(
            new CollectionExportField("id", "ID", true, true, "number", "id"),
            new CollectionExportField("nomeCompleto", "Nome", true, true, "string", "nomeCompleto"),
            new CollectionExportField("departamentoNome", "Departamento", true, true, "string", "departamentoNome"),
            new CollectionExportField("cargoNome", "Cargo", true, true, "string", "cargoNome"),
            new CollectionExportField(
                    "ativo",
                    "Ativo",
                    true,
                    true,
                    "boolean",
                    "ativo",
                    null,
                    new CollectionExportFieldPresentation("boolean", null, null, "pt-BR", null, "Ativo", "Inativo", null)
            ),
            new CollectionExportField("estadoCivil", "Estado Civil", true, true, "string", "estadoCivil"),
            new CollectionExportField(
                    "salario",
                    "Salario",
                    true,
                    true,
                    "currency",
                    "salario",
                    null,
                    new CollectionExportFieldPresentation("currency", null, "BRL", "pt-BR", null, null, null, null)
            ),
            new CollectionExportField("dataAdmissao", "Data de Admissao", true, true, "date", "dataAdmissao", "dd/MM/yyyy", null),
            new CollectionExportField("departamentoId", "Departamento ID", false, true, "number", "departamentoId"),
            new CollectionExportField("cargoId", "Cargo ID", false, true, "number", "cargoId")
    );
    private static final CollectionExportCapability EXPORT_CAPABILITY = new CollectionExportCapability(
            List.of(CollectionExportFormat.CSV, CollectionExportFormat.JSON, CollectionExportFormat.EXCEL),
            List.of(
                    CollectionExportScope.AUTO,
                    CollectionExportScope.SELECTED,
                    CollectionExportScope.FILTERED,
                    CollectionExportScope.CURRENT_PAGE,
                    CollectionExportScope.ALL
            ),
            Map.of(
                    CollectionExportFormat.CSV.value(), EXPORT_MAX_ROWS,
                    CollectionExportFormat.JSON.value(), EXPORT_MAX_ROWS,
                    CollectionExportFormat.EXCEL.value(), EXPORT_MAX_ROWS
            ),
            false
    );

    /**
     * Fonte canonica de Entity Lookup para selecao de funcionarios.
     *
     * <p>Este descritor e intencionalmente mais rico que o endpoint legado
     * {@code /options/filter}: ele ensina hosts Praxis a publicar uma entidade de negocio como
     * lookup governado, com busca semantica, detalhes navegaveis e politica de selecao. Campos
     * sensiveis como CPF, telefone e salario ficam fora da busca e da descricao para preservar a
     * governanca LGPD do recurso.</p>
     */
    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(Funcionario.class, new OptionSourceDescriptor(
                    EMPLOYEE_OPTION_SOURCE_KEY,
                    OptionSourceType.RESOURCE_ENTITY,
                    ApiPaths.HumanResources.FUNCIONARIOS,
                    null,
                    "id",
                    "nomeCompleto",
                    "id",
                    List.of(),
                    lookupPolicy(),
                    new EntityLookupDescriptor(
                            EMPLOYEE_OPTION_SOURCE_KEY,
                            null,
                            List.of("cargo.nome", "departamento.nome"),
                            null,
                            null,
                            null,
                            List.of("nomeCompleto", "cargo.nome", "departamento.nome"),
                            null,
                            new LookupSelectionPolicy(
                                    "ativo",
                                    null,
                                    List.of(),
                                    List.of(),
                                    true,
                                    "Funcionario inativo preservado apenas para reidratacao de valores existentes.",
                                    "Selecione um funcionario ativo."
                            ),
                            new LookupCapabilities(true, true, true, false, false, true, false, false, false, true),
                            new LookupDetailDescriptor(null, null, null, "surface", "view", "drawer", "praxis-dynamic-form", "view"),
                            new LookupDisplayDescriptor(
                                    "directory",
                                    "form",
                                    "comfortable",
                                    "compact",
                                    "list",
                                    "nomeCompleto",
                                    List.of(
                                            new LookupDisplayFieldDescriptor("role", "cargo.nome", "Cargo", "work", "text", "neutral", null),
                                            new LookupDisplayFieldDescriptor("department", "departamento.nome", "Departamento", "groups", "chip", "info", null),
                                            new LookupDisplayFieldDescriptor("admissionDate", "dataAdmissao", "Admissao", "event", "date", "success", "date")
                                    ),
                                    List.of("cargo.nome", "departamento.nome", "dataAdmissao"),
                                    List.of("departamento.nome"),
                                    null,
                                    true,
                                    false,
                                    true,
                                    false,
                                    true,
                                    true,
                                    2
                            ),
                            null
                    )
            ))
            .build();

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .categoricalGroupByBucket("ativo", "ativo")
            .categoricalGroupByBucket("estadoCivil", "estadoCivil")
            .groupByBucket("cargoNome", "cargo.nome", Set.of(StatsMetric.COUNT))
            .groupByBucket("departamentoNome", "departamento.nome", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("dataAdmissao", "dataAdmissao")
            .numericHistogramMeasureField("salario", "salario")
            .build();

    private final FuncionarioMapper mapper;
    private final CollectionExportExecutor collectionExportExecutor;
    private final ResourceActionTransitionService transitionService;

    @Autowired
    public FuncionarioService(
            FuncionarioRepository repository,
            FuncionarioMapper mapper,
            CollectionExportExecutor collectionExportExecutor,
            ResourceActionTransitionService transitionService
    ) {
        super(repository, Funcionario.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Funcionario::getId);
        this.mapper = mapper;
        this.collectionExportExecutor = collectionExportExecutor;
        this.transitionService = transitionService;
    }

    /** Maintains isolated mapper/export tests that do not exercise workflow persistence. */
    public FuncionarioService(
            FuncionarioRepository repository,
            FuncionarioMapper mapper,
            CollectionExportExecutor collectionExportExecutor
    ) {
        this(repository, mapper, collectionExportExecutor, null);
    }

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    private static OptionSourcePolicy lookupPolicy() {
        return new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label");
    }

    @Override
    public Funcionario mergeUpdate(Funcionario existing, Funcionario fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }

    @Override
    public java.util.Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return java.util.Optional.of(getEntityClass().getSimpleName() + ":" + count);
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
    public CollectionExportResult exportCollection(CollectionExportRequest<FuncionarioFilterDTO> request) {
        CollectionExportRequest<FuncionarioFilterDTO> effectiveRequest = normalizeExportRequest(request);
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

    @Transactional
    public FuncionarioDTO updateProfile(Integer id, UpdateFuncionarioProfileDTO dto) {
        Funcionario existing = findEntityById(id);
        mapper.updateProfile(dto, existing);
        Funcionario saved = refreshManaged(getRepository().save(existing));
        return mapper.toDto(saved);
    }

    @Transactional
    public java.util.UUID deactivate(Integer id, FuncionarioDeactivateRequestDTO command, String actorSubject, String correlationId) {
        Funcionario employee = findEntityById(id);
        if (!Boolean.TRUE.equals(employee.getAtivo())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Employee is already inactive."
            );
        }
        long before = employee.getVersion() == null ? 0L : employee.getVersion();
        employee.setAtivo(false);
        Funcionario saved = refreshManaged(getRepository().save(employee));
        return transitionService.record("human-resources.funcionarios", id, "deactivate", "ITEM", "ATIVO", "INATIVO",
                command.getReasonCode(), command.getComment(), command.getEffectiveAt(), actorSubject, correlationId,
                before, saved.getVersion());
    }

    @Transactional
    public java.util.UUID reactivate(Integer id, FuncionarioDeactivateRequestDTO command, String actorSubject, String correlationId) {
        Funcionario employee = findEntityById(id);
        if (Boolean.TRUE.equals(employee.getAtivo())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Employee is already active.");
        }
        long before = employee.getVersion() == null ? 0L : employee.getVersion();
        employee.setAtivo(true);
        Funcionario saved = refreshManaged(getRepository().save(employee));
        return transitionService.record("human-resources.funcionarios", id, "reactivate", "ITEM", "INATIVO", "ATIVO", command.getReasonCode(), command.getComment(), command.getEffectiveAt(), actorSubject, correlationId, before, saved.getVersion());
    }

    @Override
    public StatsSupportMode getGroupByStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getTimeSeriesStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsSupportMode getDistributionStatsSupportMode() {
        return StatsSupportMode.AUTO;
    }

    @Override
    public StatsFieldRegistry getStatsFieldRegistry() {
        return STATS_FIELDS;
    }

    private ExportRows resolveExportRows(CollectionExportRequest<FuncionarioFilterDTO> request) {
        CollectionExportScope scope = resolveScope(request);
        return switch (scope) {
            case SELECTED -> resolveSelectedRows(request);
            case CURRENT_PAGE -> {
                Page<FuncionarioDTO> page = filter(filtersOrEmpty(request), pageableFromRequest(request), null);
                yield new ExportRows(
                        page.getContent(),
                        page.getTotalElements(),
                        page.getTotalElements() > page.getContent().size(),
                        effectiveMaxRows(request),
                        requestedMaxRowsExceedsServerLimit(request)
                );
            }
            case FILTERED, ALL -> {
                Page<FuncionarioDTO> page = filter(filtersOrEmpty(request), maxRowsPageable(request), null);
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

    private CollectionExportScope resolveScope(CollectionExportRequest<FuncionarioFilterDTO> request) {
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

    private ExportRows resolveSelectedRows(CollectionExportRequest<FuncionarioFilterDTO> request) {
        int maxRows = effectiveMaxRows(request);
        if (request.selection() != null && Boolean.TRUE.equals(request.selection().allMatchingSelected())) {
            Page<FuncionarioDTO> matching = filter(filtersOrEmpty(request), maxRowsPageable(request), null);
            List<Integer> excluded = toIntegerIds(request.selection().excludedKeys());
            List<FuncionarioDTO> rows = excluded.isEmpty()
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

    private Pageable pageableFromRequest(CollectionExportRequest<FuncionarioFilterDTO> request) {
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

    private Pageable maxRowsPageable(CollectionExportRequest<FuncionarioFilterDTO> request) {
        return PageRequest.of(0, effectiveMaxRows(request), sortFromRequest(request));
    }

    private Sort sortFromRequest(CollectionExportRequest<FuncionarioFilterDTO> request) {
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
            case "nomeCompleto" -> "nomeCompleto";
            case "departamentoNome" -> "departamento.nome";
            case "cargoNome" -> "cargo.nome";
            case "ativo" -> "ativo";
            case "estadoCivil" -> "estadoCivil";
            case "salario" -> "salario";
            case "dataAdmissao" -> "dataAdmissao";
            case "departamentoId" -> "departamento.id";
            case "cargoId" -> "cargo.id";
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

    private FuncionarioFilterDTO filtersOrEmpty(CollectionExportRequest<FuncionarioFilterDTO> request) {
        return request.filters() != null ? request.filters() : new FuncionarioFilterDTO();
    }

    private int effectiveMaxRows(CollectionExportRequest<FuncionarioFilterDTO> request) {
        int requested = request.maxRows() != null && request.maxRows() > 0 ? request.maxRows() : EXPORT_MAX_ROWS;
        return Math.min(requested, EXPORT_MAX_ROWS);
    }

    private boolean requestedMaxRowsExceedsServerLimit(CollectionExportRequest<FuncionarioFilterDTO> request) {
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

    private Object valueFor(FuncionarioDTO row, CollectionExportField field) {
        String key = field.valuePath() != null && !field.valuePath().isBlank()
                ? field.valuePath()
                : field.key();
        return switch (key) {
            case "id" -> row.getId();
            case "nomeCompleto" -> row.getNomeCompleto();
            case "departamentoNome" -> row.getDepartamentoNome();
            case "cargoNome" -> row.getCargoNome();
            case "ativo" -> row.getAtivo();
            case "estadoCivil" -> estadoCivilLabel(row.getEstadoCivil());
            case "salario" -> row.getSalario();
            case "dataAdmissao" -> row.getDataAdmissao();
            case "departamentoId" -> row.getDepartamentoId();
            case "cargoId" -> row.getCargoId();
            default -> "";
        };
    }

    private String ativoLabel(Boolean ativo) {
        if (ativo == null) {
            return "";
        }
        return ativo ? "Ativo" : "Inativo";
    }

    private String estadoCivilLabel(Object estadoCivil) {
        if (estadoCivil == null) {
            return "";
        }
        return switch (estadoCivil.toString()) {
            case "SOLTEIRO" -> "Solteiro";
            case "CASADO" -> "Casado";
            case "DIVORCIADO" -> "Divorciado";
            case "VIUVO" -> "Viúvo";
            case "UNIAO_ESTAVEL" -> "União estável";
            default -> estadoCivil.toString();
        };
    }

    private String currencyLabel(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NumberFormat.getCurrencyInstance(EXPORT_LOCALE).format(value);
    }

    private String dateLabel(LocalDate value) {
        if (value == null) {
            return "";
        }
        return EXPORT_DATE_FORMAT.format(value);
    }

    private CollectionExportRequest<FuncionarioFilterDTO> normalizeExportRequest(
            CollectionExportRequest<FuncionarioFilterDTO> request
    ) {
        CollectionExportRequest<FuncionarioFilterDTO> source = request == null
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
                : "funcionarios." + extensionFor(format);
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
            CollectionExportRequest<FuncionarioFilterDTO> request,
            ExportRows exportRows
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resourceKey", "human-resources.funcionarios");
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

    /** Recarrega a entidade apos salvar para devolver o estado efetivamente persistido. */
    private Funcionario refreshManaged(Funcionario entity) {
        if (getEntityManager() == null) {
            return entity;
        }
        getEntityManager().flush();
        Funcionario managed = getEntityManager().contains(entity) ? entity : getEntityManager().merge(entity);
        getEntityManager().refresh(managed);
        return managed;
    }

    private record ExportRows(
            List<FuncionarioDTO> rows,
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
