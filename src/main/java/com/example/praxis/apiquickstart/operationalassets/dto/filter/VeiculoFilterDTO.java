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
                + "Usado para localizar veiculos por designacao, tipo, responsavel, capacidade e disponibilidade operacional.")
public class VeiculoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do veículo", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Digite parte do nome ou designação do veículo.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome, matricula ou designacao operacional do veiculo.")
    private String nome;

    @UISchema(label = "Tipo de veículo", controlType = FieldControlType.SELECT, order = 20,
            helpText = "Filtra por um único tipo de veículo.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Tipo de plataforma do veiculo, como aereo, terrestre ou outro valor do catalogo VeiculoTipo.")
    private VeiculoTipo tipo;

    @UISchema(label = "Responsável", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Selecione o colaborador responsável pela frota ou veículo.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "proprietario.id")
    @Schema(
            description = "Colaborador responsavel pela custodia ou gestao operacional do veiculo.")
    private Integer proprietarioId;

    @UISchema(label = "Status do veículo", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Filtra por um único estado operacional do veículo.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status mecanico ou operacional que indica se o veiculo pode ser escalado para missao.")
    private VeiculoStatus status;

    @UISchema(label = "Capacidade", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 50,
            helpText = "Defina uma faixa de capacidade nominal para passageiros, tripulação ou carga.", icon = "location_city")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "capacidade")
    @Schema(
            description = "Faixa de capacidade nominal do veículo para passageiros, tripulação ou carga.")
    private List<Integer> capacidadeBetween;

    @UISchema(label = "Mostrar tipos", controlType = FieldControlType.SELECT, order = 60,
            helpText = "Mostra apenas veículos dos tipos selecionados.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "tipo")
    @Schema(
            description = "Conjunto de tipos de veículo que devem aparecer no resultado da busca.")
    private List<VeiculoTipo> tiposIn;

    @UISchema(label = "Mostrar status", controlType = FieldControlType.SELECT, order = 70,
            helpText = "Mostra apenas veículos nos status selecionados.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "status")
    @Schema(
            description = "Conjunto de estados operacionais que devem aparecer no resultado da busca.")
    private List<VeiculoStatus> statusIn;

    @UISchema(label = "Ocultar status", controlType = FieldControlType.SELECT, order = 80,
            helpText = "Remove do resultado os veículos nos status selecionados.", icon = "toggle_off")
    @Filterable(operation = Filterable.FilterOperation.NOT_IN, relation = "status")
    @Schema(
            description = "Conjunto de estados operacionais que devem ser removidos do resultado da busca.")
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
