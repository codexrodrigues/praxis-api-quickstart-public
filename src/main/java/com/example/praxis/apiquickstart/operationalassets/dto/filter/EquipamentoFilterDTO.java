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
                + "Usado para localizar ativos por designacao, categoria, custodiante, status e faixa de resistencia.")
public class EquipamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do equipamento", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Busca por nome, designação ou apelido do equipamento.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho da designacao, nome operacional ou apelido do equipamento no inventario.")
    private String nome;

    @UISchema(label = "Tipo de equipamento", controlType = FieldControlType.SELECT, order = 20,
            helpText = "Restringe a busca à categoria do item operacional.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Categoria tatica do equipamento, restringindo a busca a um valor do catalogo EquipamentoTipo.")
    private EquipamentoTipo tipo;

    @UISchema(label = "Responsável", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Filtra equipamentos sob custódia de um colaborador ou herói.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "proprietario.id")
    @Schema(
            description = "Colaborador ou operador que detem a custodia formal do equipamento.")
    private Integer proprietarioId;

    @UISchema(label = "Status do equipamento", controlType = FieldControlType.SELECT, order = 40,
            helpText = "Mostra itens conforme disponibilidade, manutenção ou perda.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status de disponibilidade do equipamento, como operacional, em manutencao ou perdido.")
    private EquipamentoStatus status;

    @UISchema(label = "Faixa de resistência", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 50,
            helpText = "Filtra por intervalo de durabilidade ou capacidade de absorção do equipamento.", icon = "stacked_bar_chart")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "resistencia")
    @Schema(
            description = "Faixa de resistencia, durabilidade ou capacidade de absorcao usada para comparar equipamentos.")
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
