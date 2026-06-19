package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "IdentidadeSecretaFilterDTO",
        description = "Criterios de busca em identidades, codinomes e alter egos vinculados a colaboradores. "
                + "Apoia governanca de exposicao publica, franquia de origem e relacao com o cadastro civil.")
public class IdentidadeSecretaFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Buscar identidade secreta vinculada a um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Colaborador civil ao qual a identidade, codinome ou alter ego esta vinculado.")
    private Integer funcionarioId;

    @UISchema(label = "Codinome", controlType = FieldControlType.INPUT, maxLength = 120, order = 20, helpText = "Filtrar por nome de guerra ou alter ego.", icon = "theater_comedy")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do codinome, nome de guerra ou marca publica usada para identificar o colaborador em operacoes e midia.")
    private String codinome;

    @UISchema(label = "Universo", controlType = FieldControlType.INPUT, maxLength = 120, order = 30, helpText = "Filtrar por universo ou franquia de origem.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do universo, franquia ou continuidade de origem associado a identidade operacional.")
    private String universo;

    @UISchema(label = "Exposição Pública", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 40, helpText = "Filtrar por nível de exposição (público ou secreto).", icon = "visibility")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Indicador que separa identidades assumidamente publicas de perfis protegidos ou reclusos.")
    private Boolean exposicaoPublica;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getCodinome() { return codinome; }
    public void setCodinome(String codinome) { this.codinome = codinome; }
    public String getUniverso() { return universo; }
    public void setUniverso(String universo) { this.universo = universo; }
    public Boolean getExposicaoPublica() { return exposicaoPublica; }
    public void setExposicaoPublica(Boolean exposicaoPublica) { this.exposicaoPublica = exposicaoPublica; }
}
