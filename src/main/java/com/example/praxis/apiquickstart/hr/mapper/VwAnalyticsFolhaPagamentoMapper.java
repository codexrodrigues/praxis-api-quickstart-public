package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.VwAnalyticsFolhaPagamentoDTO;
import com.example.praxis.apiquickstart.hr.entity.VwAnalyticsFolhaPagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface VwAnalyticsFolhaPagamentoMapper {
    VwAnalyticsFolhaPagamentoDTO toDto(VwAnalyticsFolhaPagamento entity);

    @Mapping(target = "cargoId", ignore = true)
    @Mapping(target = "departamentoId", ignore = true)
    @Mapping(target = "equipeId", ignore = true)
    @Mapping(target = "papelEquipe", ignore = true)
    @Mapping(target = "baseId", ignore = true)
    @Mapping(target = "tipoBase", ignore = true)
    @Mapping(target = "sigiloBase", ignore = true)
    @Mapping(target = "qtdProventos", ignore = true)
    @Mapping(target = "qtdDescontos", ignore = true)
    @Mapping(target = "qtdTiposEvento", ignore = true)
    @Mapping(target = "saldoLiquidoVsBruto", ignore = true)
    @Mapping(target = "pctAdicionaisSobreBruto", ignore = true)
    @Mapping(target = "pctEventosDescontoSobreBruto", ignore = true)
    @Mapping(target = "eventosDescricao", ignore = true)
    VwAnalyticsFolhaPagamento toEntity(VwAnalyticsFolhaPagamentoDTO dto);

    void updateEntity(VwAnalyticsFolhaPagamento source, @MappingTarget VwAnalyticsFolhaPagamento target);
}
