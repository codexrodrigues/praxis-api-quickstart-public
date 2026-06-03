package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.AcordoStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "AcordosRegulatorioFilterDTO",
        description = "Criterios de busca no registo de acordos/ tratados com poderes publicos (nao e o instrumento a assinar so com filtrar). "
                + "GenericFilter / POST /filter (demo).")
public class AcordosRegulatorioFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome curto do acordo; LIKE (demo).")
    private String nome;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 120, order = 20, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Orgao ou territorio; LIKE (jurisdicao) (demo).")
    private String jurisdicao;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Ciclo de vigencia/ homologacao; EQUAL AcordoStatus (demo).")
    private AcordoStatus status;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 4000, order = 40, icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa no texto de sumario ou clausulas; LIKE (demo).")
    private String descricao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getJurisdicao() { return jurisdicao; }
    public void setJurisdicao(String jurisdicao) { this.jurisdicao = jurisdicao; }
    public AcordoStatus getStatus() { return status; }
    public void setStatus(AcordoStatus status) { this.status = status; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
