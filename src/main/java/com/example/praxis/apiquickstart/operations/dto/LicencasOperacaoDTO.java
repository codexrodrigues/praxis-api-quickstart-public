package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.LicencaNivel;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.LocalDate;

@Schema(
        name = "LicencasOperacaoDTO",
        description = "Alvara ou licenca operacional (acordo, heroi ou equipe, classe, janela de vigencia). "
                + "Materializa a autorizacao concedida por acordo regulatorio a um colaborador, equipe ou ambos.")
public class LicencasOperacaoDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotNull
    @UISchema(label = "Acordo Regulatório", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.ACORDOS_REGULATORIOS_AGREEMENT_LOOKUP_OPTIONS, required = true,
            tableHidden = true, icon = "gavel")
    @Schema(
            description = "Acordo regulatorio que fundamenta e outorga a licenca operacional.")
    private Integer acordoId;

    @UISchema(label = "Funcionário", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, icon = "badge")
    @Schema(
            description = "Colaborador titular quando a licenca operacional e pessoal.")
    private Integer funcionarioId;

    @UISchema(label = "Equipe", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.EQUIPES_TEAM_LOOKUP_OPTIONS,
            tableHidden = true, icon = "groups")
    @Schema(
            description = "Equipe tatica licenciada, como alternativa ou complemento a um titular individual.")
    private Integer equipeId;

    @UISchema(label = "Acordo Regulatório", readOnly = true, formHidden = true, icon = "gavel")
    @Schema(
            description = "Titulo do acordo denormalizado (read model).")
    private String acordoNome;
    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, icon = "badge")
    @Schema(
            description = "Nome do colaborador denormalizado (read model).")
    private String funcionarioNome;
    @UISchema(label = "Equipe", readOnly = true, formHidden = true, icon = "groups")
    @Schema(
            description = "Nome da equipe denormalizado (read model).")
    private String equipeNome;

    @UISchema(label = "Nível", controlType = FieldControlType.SELECT, icon = "trending_up")
    @Schema(
            description = "Classe de autorizacao (Poder restrito, TSI, etc.); LicencaNivel.")
    private LicencaNivel nivel;

    @NotNull
    @UISchema(label = "Válido De", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, required = true, icon = "event")
    @Schema(
            description = "Inicio de vigencia da licenca.")
    private LocalDate validoDe;

    @UISchema(label = "Válido Até", type = FieldDataType.DATE, controlType = FieldControlType.DATE_PICKER, icon = "event")
    @Schema(
            description = "Fim de vigencia da licenca; nulo indica autorizacao sem termino registrado ou em renovacao.")
    private LocalDate validoAte;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAcordoId() { return acordoId; }
    public void setAcordoId(Integer acordoId) { this.acordoId = acordoId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public Integer getEquipeId() { return equipeId; }
    public void setEquipeId(Integer equipeId) { this.equipeId = equipeId; }
    public String getAcordoNome() { return acordoNome; }
    public void setAcordoNome(String acordoNome) { this.acordoNome = acordoNome; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getEquipeNome() { return equipeNome; }
    public void setEquipeNome(String equipeNome) { this.equipeNome = equipeNome; }
    public LicencaNivel getNivel() { return nivel; }
    public void setNivel(LicencaNivel nivel) { this.nivel = nivel; }
    public LocalDate getValidoDe() { return validoDe; }
    public void setValidoDe(LocalDate validoDe) { this.validoDe = validoDe; }
    public LocalDate getValidoAte() { return validoAte; }
    public void setValidoAte(LocalDate validoAte) { this.validoAte = validoAte; }
}


