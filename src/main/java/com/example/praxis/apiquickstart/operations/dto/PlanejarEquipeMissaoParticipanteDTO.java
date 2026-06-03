package com.example.praxis.apiquickstart.operations.dto;

import com.example.praxis.apiquickstart.operations.enums.PapelMissao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "PlanejarEquipeMissaoParticipanteDTO",
        description = "Elemento de equipe a alocar: heroi, papel, ordem e designacao de lider no planejamento. "
                + "OpenAPI 3.1 e x-ui (demo).")
public class PlanejarEquipeMissaoParticipanteDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(
            label = "Funcionario",
            controlType = FieldControlType.ENTITY_LOOKUP,
            required = true,
            valueField = "id",
            displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            icon = "badge"
    )
    @Schema(
            description = "FK; colaborador a escalar (funcionarioId).")
    private Integer funcionarioId;

    @UISchema(label = "Nome", readOnly = true, icon = "badge")
    @Schema(
            description = "Nome denormalizado para cards (read model).")
    private String funcionarioNome;

    @NotNull
    @UISchema(label = "Papel", controlType = FieldControlType.SELECT, required = true, icon = "flag")
    @Schema(
            description = "Papel tatico na operacao; PapelMissao.")
    private PapelMissao papel;

    @UISchema(label = "Ordem", controlType = FieldControlType.NUMERIC_TEXT_BOX, readOnly = true, icon = "flag")
    @Schema(
            description = "Ordem de exibicao/ insercao; preenchida pelo servico de planejamento.")
    private Integer ordem;

    @UISchema(label = "Principal", controlType = FieldControlType.TOGGLE, icon = "toggle_on")
    @Schema(
            description = "Marca o lider/ face da operacao no conjunto proposto.")
    private Boolean principal;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public PapelMissao getPapel() { return papel; }
    public void setPapel(PapelMissao papel) { this.papel = papel; }
    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }
    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }
}
