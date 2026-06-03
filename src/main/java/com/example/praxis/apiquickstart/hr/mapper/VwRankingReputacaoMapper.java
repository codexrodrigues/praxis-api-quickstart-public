package com.example.praxis.apiquickstart.hr.mapper;

import com.example.praxis.apiquickstart.hr.dto.VwRankingReputacaoDTO;
import com.example.praxis.apiquickstart.hr.entity.VwRankingReputacao;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.praxisplatform.uischema.mapper.config.CorporateMapperConfig;

@Mapper(componentModel = "spring", config = CorporateMapperConfig.class)
public interface VwRankingReputacaoMapper {
    VwRankingReputacaoDTO toDto(VwRankingReputacao entity);
    VwRankingReputacao toEntity(VwRankingReputacaoDTO dto);
    void updateEntity(VwRankingReputacao source, @MappingTarget VwRankingReputacao target);
}

