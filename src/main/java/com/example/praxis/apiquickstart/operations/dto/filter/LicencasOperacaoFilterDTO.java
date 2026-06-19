package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.enums.LicencaNivel;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "LicencasOperacaoFilterDTO",
        description = "Criterios de busca em licencas e autorizacoes operacionais. "
                + "Relaciona acordos regulatorios, colaboradores, equipes, nivel de autorizacao e vigencia.")
public class LicencasOperacaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Acordo regulatório", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_OPTIONS,
            helpText = "Mostra licenças emitidas a partir de um acordo específico.", icon = "gavel")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "acordo.id")
    @Schema(
            description = "Acordo regulatorio que fundamenta a emissao da licenca operacional.")
    private Integer acordoId;

    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Filtra licenças concedidas a um colaborador ou herói.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador ou heroi ao qual a licenca operacional foi concedida.")
    private Integer funcionarioId;

    @UISchema(label = "Equipe", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 30,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_OPTIONS,
            helpText = "Filtra autorizações concedidas a uma equipe.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipe.id")
    @Schema(
            description = "Equipe tatica contemplada pela autorizacao operacional.")
    private Integer equipeId;

    @UISchema(label = "Nível da licença", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Seleciona o nível ou alcance operacional da autorização.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Nivel ou alcance operacional autorizado pela licenca.")
    private LicencaNivel nivel;

    @UISchema(label = "Válida a partir de", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 50,
            helpText = "Filtra pela janela de início de validade da licença.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "validoDe")
    @Schema(
            description = "Janela de inicio de vigencia da licenca operacional.")
    private List<LocalDate> validoDeBetween;

    @UISchema(label = "Válida até", type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 60,
            helpText = "Filtra pela janela de expiração ou vencimento da licença.", icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "validoAte")
    @Schema(
            description = "Janela de termino, expiracao ou vencimento da licenca operacional.")
    private List<LocalDate> validoAteBetween;

    public Integer getAcordoId() { return acordoId; }
    public void setAcordoId(Integer acordoId) { this.acordoId = acordoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getEquipeId() { return equipeId; }
    public void setEquipeId(Integer equipeId) { this.equipeId = equipeId; }
    public LicencaNivel getNivel() { return nivel; }
    public void setNivel(LicencaNivel nivel) { this.nivel = nivel; }
    public List<LocalDate> getValidoDeBetween() { return validoDeBetween; }
    public void setValidoDeBetween(List<LocalDate> validoDeBetween) { this.validoDeBetween = validoDeBetween; }
    public List<LocalDate> getValidoAteBetween() { return validoAteBetween; }
    public void setValidoAteBetween(List<LocalDate> validoAteBetween) { this.validoAteBetween = validoAteBetween; }
}
