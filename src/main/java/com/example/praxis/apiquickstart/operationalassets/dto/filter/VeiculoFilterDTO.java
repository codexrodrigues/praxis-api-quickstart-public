package com.example.praxis.apiquickstart.operationalassets.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operationalassets.enums.VeiculoStatus;
import com.example.praxis.apiquickstart.operationalassets.enums.VeiculoTipo;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.util.List;

@Schema(
        name = "VeiculoFilterDTO",
        description = "Criterios de busca na frota (veiculos de missao); nao e o ativo a editar so com filtrar. "
                + "Suporta multiplos tipos/ status; GenericFilter / POST /filter (demo).")
public class VeiculoFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome ou designacao (ex.: Jato-7); LIKE (demo).")
    private String nome;

    @UISchema(controlType = FieldControlType.SELECT, order = 20, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Um tipo; EQUAL VeiculoTipo (aereo, terrestre, etc.) (demo).")
    private VeiculoTipo tipo;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "proprietario.id")
    @Schema(
            description = "Frota vinculada a um colaborador/ proprietario; EQUAL (FK) (demo).")
    private Integer proprietarioId;

    @UISchema(controlType = FieldControlType.SELECT, order = 40, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Situacao mecanica/ operacional unica; EQUAL VeiculoStatus (demo).")
    private VeiculoStatus status;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 50, icon = "location_city")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "capacidade")
    @Schema(
            description = "Passageiros ou carga nominal; BETWEEN (demo).")
    private List<Integer> capacidadeBetween;

    @UISchema(label = "Tipos (Incluir)", controlType = FieldControlType.SELECT, order = 60, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de tipos; operacao IN (demo).")
    private List<VeiculoTipo> tiposIn;

    @UISchema(label = "Status (Incluir)", controlType = FieldControlType.SELECT, order = 70, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN)
    @Schema(
            description = "Uniao de estados; operacao IN (demo).")
    private List<VeiculoStatus> statusIn;

    @UISchema(label = "Status (Excluir)", controlType = FieldControlType.SELECT, order = 80, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN)
    @Schema(
            description = "Excluir estados; NOT_IN (demo).")
    private List<VeiculoStatus> statusNotIn;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public VeiculoTipo getTipo() { return tipo; }
    public void setTipo(VeiculoTipo tipo) { this.tipo = tipo; }
    public Integer getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(Integer proprietarioId) { this.proprietarioId = proprietarioId; }
    public VeiculoStatus getStatus() { return status; }
    public void setStatus(VeiculoStatus status) { this.status = status; }
    public List<Integer> getCapacidadeBetween() { return capacidadeBetween; }
    public void setCapacidadeBetween(List<Integer> capacidadeBetween) { this.capacidadeBetween = capacidadeBetween; }

    public List<VeiculoTipo> getTiposIn() { return tiposIn; }
    public void setTiposIn(List<VeiculoTipo> tiposIn) { this.tiposIn = tiposIn; }
    public List<VeiculoStatus> getStatusIn() { return statusIn; }
    public void setStatusIn(List<VeiculoStatus> statusIn) { this.statusIn = statusIn; }
    public List<VeiculoStatus> getStatusNotIn() { return statusNotIn; }
    public void setStatusNotIn(List<VeiculoStatus> statusNotIn) { this.statusNotIn = statusNotIn; }
}
