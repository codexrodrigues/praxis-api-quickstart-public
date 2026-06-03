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
        description = "Criterios de busca em registo de identidade/alter ego (nao e o documento a alterar so por filtrar). "
                + "Conecta a cadastro de heroi; GenericFilter / POST /filter; sensivel a visibilidade publica (demo).")
public class IdentidadeSecretaFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Buscar identidade secreta vinculada a um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "funcionario.id")
    @Schema(
            description = "Funcionario civil dono do codinome; EQUAL (FK) — criterio de busca, nao a identidade fechada por si.")
    private Integer funcionarioId;

    @UISchema(label = "Codinome", controlType = FieldControlType.INPUT, maxLength = 120, order = 20, helpText = "Filtrar por nome de guerra ou alter ego.", icon = "theater_comedy")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa de nome de guerra ou marca publica; LIKE.")
    private String codinome;

    @UISchema(label = "Universo", controlType = FieldControlType.INPUT, maxLength = 120, order = 30, helpText = "Filtrar por universo ou franquia de origem.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Agrupador ficcional (universo editoral); LIKE para achar franquias/continuidades (demo).")
    private String universo;

    @UISchema(label = "Exposição Pública", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 40, helpText = "Filtrar por nível de exposição (público ou secreto).", icon = "visibility")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Recorte de herois com exposicao publica planejada (midia) vs. perfil recluso; EQUAL boolean (demo).")
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
