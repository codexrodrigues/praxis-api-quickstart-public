package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.mapper.FuncionarioMapper;
import com.example.praxis.apiquickstart.hr.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.praxisplatform.uischema.exporting.CollectionExportExecutor;
import org.praxisplatform.uischema.exporting.CollectionExportCsvOptions;
import org.praxisplatform.uischema.exporting.CollectionExportField;
import org.praxisplatform.uischema.exporting.CollectionExportFormatOptions;
import org.praxisplatform.uischema.exporting.CollectionExportLocalization;
import org.praxisplatform.uischema.exporting.CollectionExportFormat;
import org.praxisplatform.uischema.exporting.CollectionExportRequest;
import org.praxisplatform.uischema.exporting.CollectionExportScope;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceStatsTest {

    @Mock
    private FuncionarioRepository repository;

    @Mock
    private FuncionarioMapper mapper;

    @Mock
    private CollectionExportExecutor collectionExportExecutor;

    @Test
    void shouldExposeGovernedStatsCapabilitiesForFuncionarios() {
        FuncionarioService service = new FuncionarioService(repository, mapper, collectionExportExecutor);

        assertEquals(StatsSupportMode.AUTO, service.getGroupByStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getTimeSeriesStatsSupportMode());
        assertEquals(StatsSupportMode.AUTO, service.getDistributionStatsSupportMode());

        assertTrue(service.getStatsFieldRegistry().resolve("ativo").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("estadoCivil").orElseThrow().distributionTermsEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("cargoNome").orElseThrow().groupByEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("departamentoNome").orElseThrow().distributionTermsEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("dataAdmissao").orElseThrow().timeSeriesEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("salario").orElseThrow().distributionHistogramEligible());
        assertTrue(service.getStatsFieldRegistry().resolve("salario").orElseThrow().supports(StatsMetric.SUM));
    }

    @Test
    void shouldExposeCollectionExportCapabilityForFuncionarios() {
        FuncionarioService service = new FuncionarioService(repository, mapper, collectionExportExecutor);

        assertTrue(service.supportsCollectionExport());
        var capability = service.getCollectionExportCapability().orElseThrow();

        assertTrue(capability.formats().contains(CollectionExportFormat.JSON));
        assertTrue(capability.formats().contains(CollectionExportFormat.CSV));
        assertTrue(capability.formats().contains(CollectionExportFormat.EXCEL));
        assertTrue(capability.scopes().contains(CollectionExportScope.SELECTED));
        assertEquals(500, capability.maxRows().get(CollectionExportFormat.JSON.value()));
        assertEquals(500, capability.maxRows().get(CollectionExportFormat.EXCEL.value()));
    }

    @Test
    void shouldDeclareGovernedExportPresentationForSensitiveFuncionarioFields() throws Exception {
        Field field = FuncionarioService.class.getDeclaredField("DEFAULT_EXPORT_FIELDS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<CollectionExportField> fields = (List<CollectionExportField>) field.get(null);

        CollectionExportField salario = fields.stream()
                .filter(candidate -> "salario".equals(candidate.key()))
                .findFirst()
                .orElseThrow();
        CollectionExportField dataAdmissao = fields.stream()
                .filter(candidate -> "dataAdmissao".equals(candidate.key()))
                .findFirst()
                .orElseThrow();
        CollectionExportField ativo = fields.stream()
                .filter(candidate -> "ativo".equals(candidate.key()))
                .findFirst()
                .orElseThrow();

        assertEquals("currency", salario.type());
        assertEquals("currency", salario.presentation().semanticType());
        assertEquals("BRL", salario.presentation().currency());
        assertEquals("pt-BR", salario.presentation().locale());
        assertEquals("dd/MM/yyyy", dataAdmissao.format());
        assertEquals("Ativo", ativo.presentation().trueLabel());
        assertEquals("Inativo", ativo.presentation().falseLabel());
    }

    @Test
    void shouldRenderEstadoCivilAsBusinessLabelForExports() throws Exception {
        FuncionarioService service = new FuncionarioService(repository, mapper, collectionExportExecutor);
        Method method = FuncionarioService.class.getDeclaredMethod("estadoCivilLabel", Object.class);
        method.setAccessible(true);

        assertEquals("União estável", method.invoke(service, "UNIAO_ESTAVEL"));
        assertEquals("Viúvo", method.invoke(service, "VIUVO"));
        assertEquals("Divorciado", method.invoke(service, "DIVORCIADO"));
    }

    @Test
    void shouldRenderExportValuesAsBusinessLabels() throws Exception {
        FuncionarioService service = new FuncionarioService(repository, mapper, collectionExportExecutor);
        Method ativoLabel = FuncionarioService.class.getDeclaredMethod("ativoLabel", Boolean.class);
        Method currencyLabel = FuncionarioService.class.getDeclaredMethod("currencyLabel", BigDecimal.class);
        Method dateLabel = FuncionarioService.class.getDeclaredMethod("dateLabel", LocalDate.class);
        ativoLabel.setAccessible(true);
        currencyLabel.setAccessible(true);
        dateLabel.setAccessible(true);

        assertEquals("Ativo", ativoLabel.invoke(service, true));
        assertEquals("Inativo", ativoLabel.invoke(service, false));
        assertEquals("R$\u00a041.000,00", currencyLabel.invoke(service, new BigDecimal("41000.00")));
        assertEquals("13/06/2022", dateLabel.invoke(service, LocalDate.of(2022, 6, 13)));
    }

    @Test
    void shouldPreserveExportFormatOptionsAndLocalizationWhenDefaultingFileName() throws Exception {
        FuncionarioService service = new FuncionarioService(repository, mapper, collectionExportExecutor);
        CollectionExportFormatOptions formatOptions = new CollectionExportFormatOptions(
                new CollectionExportCsvOptions(null, null, null, null, true, null),
                null
        );
        CollectionExportLocalization localization = new CollectionExportLocalization("pt-BR", "America/Sao_Paulo");
        CollectionExportRequest<?> request = new CollectionExportRequest<>(
                "table",
                "funcionarios",
                "/api/human-resources/funcionarios",
                CollectionExportFormat.CSV,
                CollectionExportScope.CURRENT_PAGE,
                null,
                List.of(),
                null,
                null,
                null,
                Map.of(),
                true,
                true,
                null,
                null,
                formatOptions,
                localization,
                Map.of()
        );

        Method method = FuncionarioService.class.getDeclaredMethod("normalizeExportRequest", CollectionExportRequest.class);
        method.setAccessible(true);
        CollectionExportRequest<?> normalized = (CollectionExportRequest<?>) method.invoke(service, request);

        assertEquals("funcionarios.csv", normalized.fileName());
        assertEquals(formatOptions, normalized.formatOptions());
        assertEquals(localization, normalized.localization());
    }
}
