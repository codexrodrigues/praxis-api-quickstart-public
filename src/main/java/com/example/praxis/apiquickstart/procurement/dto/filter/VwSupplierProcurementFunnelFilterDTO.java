package com.example.praxis.apiquickstart.procurement.dto.filter;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "VwSupplierProcurementFunnelFilterDTO",
        description = "Recorte da visao cumulativa de procurement por empresa e etapa canonica.")
public class VwSupplierProcurementFunnelFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Empresa", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP,
            order = 10, valueField = "id", displayField = "label",
            endpoint = ApiPaths.Procurement.COMPANIES_COMPANY_LOOKUP_OPTIONS,
            helpText = "Seleciona a empresa compradora sem duplicar o cadastro corporativo.", icon = "business")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(description = "Empresa compradora usada para isolar um funil cumulativo.")
    private Integer companyId;

    @UISchema(label = "Etapas", type = FieldDataType.NUMBER, controlType = FieldControlType.MULTI_SELECT,
            order = 20, formHidden = true, icon = "filter_alt")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "stageOrder")
    @Schema(description = "Ordens canonicas das etapas que devem permanecer no recorte tabular.")
    private List<Integer> stageOrdersIn;

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public List<Integer> getStageOrdersIn() { return stageOrdersIn; }
    public void setStageOrdersIn(List<Integer> stageOrdersIn) { this.stageOrdersIn = stageOrdersIn; }
}
