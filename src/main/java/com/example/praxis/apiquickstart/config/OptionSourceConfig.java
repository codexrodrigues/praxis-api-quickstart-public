package com.example.praxis.apiquickstart.config;

import com.example.praxis.apiquickstart.hr.service.CargoService;
import com.example.praxis.apiquickstart.hr.service.DepartamentoService;
import com.example.praxis.apiquickstart.hr.service.VwAnalyticsFolhaPagamentoService;
import com.example.praxis.apiquickstart.hr.service.FolhasPagamentoService;
import com.example.praxis.apiquickstart.hr.service.FuncionarioService;
import com.example.praxis.apiquickstart.hr.service.HabilidadeService;
import com.example.praxis.apiquickstart.operations.service.AcordosRegulatorioService;
import com.example.praxis.apiquickstart.operations.service.BaseService;
import com.example.praxis.apiquickstart.operations.service.EquipeService;
import com.example.praxis.apiquickstart.operations.service.IncidenteService;
import com.example.praxis.apiquickstart.operations.service.MissaoService;
import com.example.praxis.apiquickstart.operationalassets.service.EquipamentoService;
import com.example.praxis.apiquickstart.operationalassets.service.VeiculoService;
import com.example.praxis.apiquickstart.procurement.service.ProcurementCompanyService;
import com.example.praxis.apiquickstart.procurement.service.ProcurementContractService;
import com.example.praxis.apiquickstart.procurement.service.ProcurementProductService;
import com.example.praxis.apiquickstart.procurement.service.ProcurementSupplierService;
import com.example.praxis.apiquickstart.riskintelligence.service.AmeacaService;
import com.example.praxis.apiquickstart.riskintelligence.service.VwIndicadoresIncidenteService;
import com.example.praxis.apiquickstart.hr.service.VwPerfilHeroiService;
import org.praxisplatform.uischema.options.OptionSourceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * Publica o registro de option sources dinamicos usados pelos schemas enriquecidos.
 *
 * <p>No quickstart, algumas views read-only expoem filtros e campos derivados cujo conjunto de
 * opcoes nao e estatico. Este bean consolida esses fornecedores em um
 * {@link OptionSourceRegistry}, permitindo que o {@code praxis-metadata-starter} publique os
 * endpoints de options referenciados em {@code x-ui}.</p>
 *
 * <p>Em termos pedagogicos, esta classe mostra que um backend Praxis nao precisa limitar options a
 * enums locais: ele pode registrar fontes dinamicas alinhadas com a mesma superficie metadata-driven
 * consumida pelo runtime oficial.</p>
 */
public class OptionSourceConfig {

    @Bean
    OptionSourceRegistry optionSourceRegistry() {
        return OptionSourceRegistry.merge(
                FuncionarioService.optionSources(),
                CargoService.optionSources(),
                DepartamentoService.optionSources(),
                HabilidadeService.optionSources(),
                AmeacaService.optionSources(),
                BaseService.optionSources(),
                EquipeService.optionSources(),
                AcordosRegulatorioService.optionSources(),
                IncidenteService.optionSources(),
                MissaoService.optionSources(),
                EquipamentoService.optionSources(),
                VeiculoService.optionSources(),
                VwAnalyticsFolhaPagamentoService.optionSources(),
                VwPerfilHeroiService.optionSources(),
                VwIndicadoresIncidenteService.optionSources(),
                ProcurementCompanyService.optionSources(),
                ProcurementSupplierService.optionSources(),
                ProcurementContractService.optionSources(),
                ProcurementProductService.optionSources(),
                FolhasPagamentoService.optionSources()
        );
    }
}
