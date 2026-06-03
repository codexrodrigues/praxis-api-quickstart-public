package com.example.praxis.apiquickstart.operationalassets.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoStatus;
import com.example.praxis.apiquickstart.operationalassets.enums.EquipamentoTipo;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.util.List;

@Schema(
        name = "EquipamentoFilterDTO",
        description = "Criterios de busca no inventario de equipamento (traje, arma, gadget); nao e o item a ajustar so com filtrar. "
                + "GenericFilter / POST /filter (demo).")
public class EquipamentoFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Designacao (ex.: luva anti-grav); LIKE (demo).")
    private String nome;

    @UISchema(controlType = FieldControlType.SELECT, order = 20, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Categoria de equipamento; EQUAL EquipamentoTipo (demo).")
    private EquipamentoTipo tipo;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "proprietario.id")
    @Schema(
            description = "Custodia por heroi/ funcionario; EQUAL proprietarioId (demo).")
    private Integer proprietarioId;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao (operacional, manutencao, perdido); EQUAL EquipamentoStatus (demo).")
    private EquipamentoStatus status;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 50, icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "resistencia")
    @Schema(
            description = "Indice de durabilidade/ absorcao; BETWEEN (1-N conforme regra) (demo).")
    private List<Integer> resistenciaBetween;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public EquipamentoTipo getTipo() { return tipo; }
    public void setTipo(EquipamentoTipo tipo) { this.tipo = tipo; }
    public Integer getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(Integer proprietarioId) { this.proprietarioId = proprietarioId; }
    public EquipamentoStatus getStatus() { return status; }
    public void setStatus(EquipamentoStatus status) { this.status = status; }
    public List<Integer> getResistenciaBetween() { return resistenciaBetween; }
    public void setResistenciaBetween(List<Integer> resistenciaBetween) { this.resistenciaBetween = resistenciaBetween; }
}
