package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsFolhaPagamentoDTO;
import com.example.praxis.apiquickstart.hr.dto.filter.VwAnalyticsFolhaPagamentoFilterDTO;
import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento;
import com.example.praxis.apiquickstart.hr.mapper.VwAnalyticsFolhaPagamentoMapper;
import com.example.praxis.apiquickstart.hr.repository.VwAnalyticsFolhaPagamentoRepository;
import org.praxisplatform.uischema.options.OptionSourceDescriptor;
import org.praxisplatform.uischema.options.OptionSourcePolicy;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.praxisplatform.uischema.options.OptionSourceType;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartReadOnlyService;
import org.praxisplatform.uischema.stats.StatsFieldRegistry;
import org.praxisplatform.uischema.stats.StatsMetric;
import org.praxisplatform.uischema.stats.StatsSupportMode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VwAnalyticsFolhaPagamentoService extends AbstractQuickstartReadOnlyService<VwAnalyticsFolhaPagamento, VwAnalyticsFolhaPagamentoDTO, Integer, VwAnalyticsFolhaPagamentoFilterDTO> {

    private static final StatsFieldRegistry STATS_FIELDS = StatsFieldRegistry.builder()
            .groupByBucket("ano", "ano", Set.of(StatsMetric.COUNT))
            .groupByBucket("mes", "mes", Set.of(StatsMetric.COUNT))
            .groupByBucket("universo", "universo", Set.of(StatsMetric.COUNT))
            .groupByBucket("cargo", "cargo", Set.of(StatsMetric.COUNT))
            .groupByBucket("departamento", "departamento", Set.of(StatsMetric.COUNT))
            .groupByBucket("equipe", "equipe", Set.of(StatsMetric.COUNT))
            .groupByBucket("base", "base", Set.of(StatsMetric.COUNT))
            .groupByBucket("payrollProfile", "payrollProfile", Set.of(StatsMetric.COUNT))
            .groupByBucket("composicaoFolha", "composicaoFolha", Set.of(StatsMetric.COUNT))
            .groupByBucket("faixaSalarioBruto", "faixaSalarioBruto", Set.of(StatsMetric.COUNT))
            .groupByBucket("faixaSalarioLiquido", "faixaSalarioLiquido", Set.of(StatsMetric.COUNT))
            .groupByBucket("faixaPctDesconto", "faixaPctDesconto", Set.of(StatsMetric.COUNT))
            .groupByBucket("faixaValorAdicionais", "faixaValorAdicionais", Set.of(StatsMetric.COUNT))
            .temporalTimeSeriesField("competencia", "competencia")
            .temporalTimeSeriesField("dataPagamento", "dataPagamento")
            .numericHistogramMeasureField("salarioBruto", "salarioBruto")
            .numericHistogramMeasureField("totalDescontos", "totalDescontos")
            .numericHistogramMeasureField("salarioLiquido", "salarioLiquido")
            .numericHistogramMeasureField("valorProventos", "valorProventos")
            .numericHistogramMeasureField("valorDescontosEventos", "valorDescontosEventos")
            .numericHistogramMeasureField("valorAdicionais", "valorAdicionais")
            .numericHistogramMeasureField("saldoEventos", "saldoEventos")
            .numericHistogramMeasureField("saldoLiquidoVsBruto", "saldoLiquidoVsBruto")
            .numericHistogramMeasureField("pctDesconto", "pctDesconto")
            .numericHistogramMeasureField("pctLiquido", "pctLiquido")
            .numericHistogramMeasureField("pctAdicionaisSobreBruto", "pctAdicionaisSobreBruto")
            .numericHistogramMeasureField("pctEventosDescontoSobreBruto", "pctEventosDescontoSobreBruto")
            .numericHistogramMeasureField("qtdEventos", "qtdEventos")
            .numericHistogramMeasureField("qtdAdicionais", "qtdAdicionais")
            .build();

    private static final OptionSourceRegistry OPTION_SOURCES = OptionSourceRegistry.builder()
            .add(VwAnalyticsFolhaPagamento.class, new OptionSourceDescriptor(
                    "universo",
                    OptionSourceType.DISTINCT_DIMENSION,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                    "universo",
                    "universo",
                    "universo",
                    "universo",
                    List.of("competenciaBetween", "dataPagamentoBetween"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .add(VwAnalyticsFolhaPagamento.class, new OptionSourceDescriptor(
                    "payrollProfile",
                    OptionSourceType.DISTINCT_DIMENSION,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                    "payrollProfile",
                    "payrollProfile",
                    "payrollProfile",
                    "payrollProfile",
                    List.of("competenciaBetween", "universo"),
                    Map.of("universo", "universoContexto"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .add(VwAnalyticsFolhaPagamento.class, new OptionSourceDescriptor(
                    "composicaoFolha",
                    OptionSourceType.DISTINCT_DIMENSION,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                    "composicaoFolha",
                    "composicaoFolha",
                    "composicaoFolha",
                    "composicaoFolha",
                    List.of("competenciaBetween", "universo", "payrollProfile"),
                    new OptionSourcePolicy(true, true, "contains", 0, 25, 100, true, false, "label")
            ))
            .add(VwAnalyticsFolhaPagamento.class, new OptionSourceDescriptor(
                    "faixaSalarioBruto",
                    OptionSourceType.CATEGORICAL_BUCKET,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                    "faixaSalarioBruto",
                    "faixaSalarioBruto",
                    "faixaSalarioBruto",
                    "faixaSalarioBruto",
                    List.of("competenciaBetween", "universo", "payrollProfile"),
                    OptionSourcePolicy.defaults()
            ))
            .add(VwAnalyticsFolhaPagamento.class, new OptionSourceDescriptor(
                    "faixaSalarioLiquido",
                    OptionSourceType.CATEGORICAL_BUCKET,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                    "faixaSalarioLiquido",
                    "faixaSalarioLiquido",
                    "faixaSalarioLiquido",
                    "faixaSalarioLiquido",
                    List.of("competenciaBetween", "universo", "payrollProfile"),
                    OptionSourcePolicy.defaults()
            ))
            .add(VwAnalyticsFolhaPagamento.class, new OptionSourceDescriptor(
                    "faixaPctDesconto",
                    OptionSourceType.CATEGORICAL_BUCKET,
                    com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.VW_ANALYTICS_FOLHA_PAGAMENTO,
                    "faixaPctDesconto",
                    "faixaPctDesconto",
                    "faixaPctDesconto",
                    "faixaPctDesconto",
                    List.of("competenciaBetween", "universo", "payrollProfile"),
                    OptionSourcePolicy.defaults()
            ))
            .build();

    private final VwAnalyticsFolhaPagamentoMapper mapper;
    private final VwAnalyticsFolhaPagamentoRepository repository;

    public VwAnalyticsFolhaPagamentoService(VwAnalyticsFolhaPagamentoRepository repository, VwAnalyticsFolhaPagamentoMapper mapper) {
        super(repository, VwAnalyticsFolhaPagamento.class, mapper::toDto, VwAnalyticsFolhaPagamento::getFolhaPagamentoId);
        this.repository = repository;
        this.mapper = mapper;
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

    public static OptionSourceRegistry optionSources() {
        return OPTION_SOURCES;
    }

    @Override
    public OptionSourceRegistry getOptionSourceRegistry() {
        return OPTION_SOURCES;
    }

    public List<VwAnalyticsFolhaPagamentoDTO> findLatestPayrollByFuncionarioId(Integer funcionarioId) {
        return repository.findTop12ByFuncionarioIdOrderByCompetenciaDesc(funcionarioId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}


