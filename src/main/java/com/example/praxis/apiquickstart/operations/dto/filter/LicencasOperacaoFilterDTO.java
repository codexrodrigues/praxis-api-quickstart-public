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
        description = "Criterios de busca em licencas/ autorizacoes operacionais (nao e a concessao a revogar so com filtrar). "
                + "Ancoradas em acordo, heroi/ equipa e validade. GenericFilter / POST /filter (demo).")
public class LicencasOperacaoFilterDTO implements GenericFilterDTO {
    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.ACORDOS_REGULATORIOS + "/options/filter", icon = "gavel")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "acordo.id")
    @Schema(
            description = "Apenas licencas vinculadas a este acordo; EQUAL (FK) (demo).")
    private Integer acordoId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Todas as licencas de um colaborador; EQUAL (demo).")
    private Integer funcionarioId;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.EQUIPES + "/options/filter", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "equipe.id")
    @Schema(
            description = "Autorizacoes para uma equipa; EQUAL (demo).")
    private Integer equipeId;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Nivel (alpha, delta, onibus); EQUAL LicencaNivel (demo).")
    private LicencaNivel nivel;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 50, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "validoDe")
    @Schema(
            description = "Inicio de validade; BETWEEN (janela) (demo).")
    private List<LocalDate> validoDeBetween;

    @UISchema(type = FieldDataType.DATE, controlType = FieldControlType.DATE_RANGE, order = 60, icon = "event")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "validoAte")
    @Schema(
            description = "Fim de validade; BETWEEN (vencimentos) (demo).")
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
