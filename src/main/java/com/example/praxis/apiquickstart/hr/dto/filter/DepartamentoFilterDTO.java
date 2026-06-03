package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "DepartamentoFilterDTO",
        description = "Criterios de busca na arvore organizacional (nao e o Departamento persistido em edicao). "
                + "GenericFilter / POST /filter no demo RH; usado em org charts e alocacao (demo).")
public class DepartamentoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome do Departamento", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Filtrar por nome do departamento.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome da unidade (ex.: Operacoes Metropolis); LIKE no titulo departamental.")
    private String nome;

    @UISchema(label = "Código", controlType = FieldControlType.INPUT, maxLength = 20, order = 20, helpText = "Filtrar por código ou sigla interna.", icon = "tag")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Codigo curto interno (ex.: OP-01); LIKE; nao e CNPJ.")
    private String codigo;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 30,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Buscar departamentos por líder responsável.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL, relation = "responsavel.id")
    @Schema(
            description = "Funcionario responsavel pelo departamento; EQUAL por id (FK logica).")
    private Integer responsavelId;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Integer responsavelId) { this.responsavelId = responsavelId; }
}
