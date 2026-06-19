package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import com.example.praxis.apiquickstart.operations.enums.PapelMissao;
import com.example.praxis.apiquickstart.operations.enums.ResultadoMissao;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "MissaoParticipanteFilterDTO",
        description = "Criterios de busca na escala de participantes de missoes. "
                + "Relaciona missao, colaborador, papel operacional, lideranca e resultado individual.")
public class MissaoParticipanteFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Missão", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
        endpoint = ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS,
            helpText = "Mostra participantes escalados em uma missão específica.", icon = "flag")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "missao.id")
    @Schema(
            description = "Missao cuja composicao de participantes deve ser consultada.")
    private Integer missaoId;

    @UISchema(label = "Participante", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 20,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            helpText = "Filtra missões atribuídas a um colaborador ou herói.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador ou heroi cuja participacao em missoes deve ser localizada.")
    private Integer funcionarioId;

    @UISchema(label = "Papel na missão", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Seleciona a função exercida pelo participante na missão.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Funcao exercida pelo participante dentro da missao.")
    private PapelMissao papel;

    @UISchema(label = "Participante principal", type = FieldDataType.BOOLEAN, controlType = FieldControlType.TOGGLE, order = 40,
            helpText = "Diferencia líderes ou referências principais dos demais participantes.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Indicador de participante principal ou referencia de lideranca dentro da missao.")
    private Boolean principal;

    @UISchema(label = "Resultado individual", controlType = FieldControlType.SELECT, order = 50,
            helpText = "Filtra pelo desfecho registrado para o participante na missão.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Desfecho individual registrado para o participante ao final da missao.")
    private ResultadoMissao resultado;

    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public PapelMissao getPapel() { return papel; }
    public void setPapel(PapelMissao papel) { this.papel = papel; }
    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }
    public ResultadoMissao getResultado() { return resultado; }
    public void setResultado(ResultadoMissao resultado) { this.resultado = resultado; }
}
