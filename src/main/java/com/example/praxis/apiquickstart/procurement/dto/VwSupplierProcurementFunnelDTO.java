package com.example.praxis.apiquickstart.procurement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@UISchema(label = "Funil de Procurement", readOnly = true, icon = "filter_alt")
@Schema(
        name = "VwSupplierProcurementFunnelDTO",
        description = "Etapa cumulativa do percurso de fornecedores por empresa, derivada de homologacao, disponibilidade operacional, contrato e pedidos persistidos. Nao materializa nem substitui policies dinamicas de selecao.")
public class VwSupplierProcurementFunnelDTO {
    @UISchema(label = "Identidade Analítica", formHidden = true, icon = "tag")
    @Schema(description = "Identidade estavel composta pela empresa e pela ordem canonica da etapa.", example = "1:4")
    private String analyticsId;

    @UISchema(label = "Empresa", type = FieldDataType.NUMBER, formHidden = true, icon = "business")
    @Schema(description = "Empresa compradora que delimita o funil cumulativo.", example = "1")
    private Integer companyId;

    @UISchema(label = "Empresa", icon = "business")
    @Schema(description = "Razao social usada como rotulo da empresa compradora.")
    private String companyName;

    @UISchema(label = "Ordem da Etapa", type = FieldDataType.NUMBER, icon = "format_list_numbered")
    @Schema(description = "Ordem canonica da etapa, de 1 (identificado) a 6 (recebido).", minimum = "1", maximum = "6")
    private Integer stageOrder;

    @UISchema(label = "Código da Etapa", formHidden = true, icon = "code")
    @Schema(description = "Chave estavel da etapa para identidade analitica e integracoes.", example = "contracted")
    private String stageKey;

    @UISchema(label = "Etapa", icon = "filter_alt")
    @Schema(description = "Rotulo humano da etapa cumulativa.", example = "Contratados")
    private String stageLabel;

    @UISchema(label = "Volume", type = FieldDataType.NUMBER, numericFormat = NumericFormat.INTEGER, icon = "numbers")
    @Schema(description = "Quantidade de fornecedores distintos que alcancaram esta etapa.", example = "18")
    private Long volume;

    @UISchema(label = "Volume Anterior", type = FieldDataType.NUMBER, numericFormat = NumericFormat.INTEGER, icon = "history")
    @Schema(description = "Volume da etapa imediatamente anterior; nulo somente na primeira etapa.")
    private Long previousVolume;

    @UISchema(label = "Conversão", type = FieldDataType.NUMBER, numericFormat = NumericFormat.PERCENT, icon = "percent")
    @Schema(description = "Razao entre o volume atual e o anterior, entre zero e um; a primeira etapa usa 1.")
    private BigDecimal conversionRate;

    @UISchema(label = "Queda", type = FieldDataType.NUMBER, numericFormat = NumericFormat.INTEGER, icon = "trending_down")
    @Schema(description = "Quantidade de fornecedores que nao avancaram desde a etapa anterior.")
    private Long dropOffCount;

    @UISchema(label = "Taxa de Queda", type = FieldDataType.NUMBER, numericFormat = NumericFormat.PERCENT, icon = "percent")
    @Schema(description = "Razao da queda sobre o volume anterior, entre zero e um; a primeira etapa usa zero.")
    private BigDecimal dropOffRate;

    public String getAnalyticsId() { return analyticsId; }
    public void setAnalyticsId(String analyticsId) { this.analyticsId = analyticsId; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public Integer getStageOrder() { return stageOrder; }
    public void setStageOrder(Integer stageOrder) { this.stageOrder = stageOrder; }
    public String getStageKey() { return stageKey; }
    public void setStageKey(String stageKey) { this.stageKey = stageKey; }
    public String getStageLabel() { return stageLabel; }
    public void setStageLabel(String stageLabel) { this.stageLabel = stageLabel; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public Long getPreviousVolume() { return previousVolume; }
    public void setPreviousVolume(Long previousVolume) { this.previousVolume = previousVolume; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }
    public Long getDropOffCount() { return dropOffCount; }
    public void setDropOffCount(Long dropOffCount) { this.dropOffCount = dropOffCount; }
    public BigDecimal getDropOffRate() { return dropOffRate; }
    public void setDropOffRate(BigDecimal dropOffRate) { this.dropOffRate = dropOffRate; }
}
