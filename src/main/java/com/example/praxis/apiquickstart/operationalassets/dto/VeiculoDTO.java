package com.example.praxis.apiquickstart.operationalassets.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operationalassets.enums.VeiculoStatus;
import com.example.praxis.apiquickstart.operationalassets.enums.VeiculoTipo;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

@Schema(
        name = "VeiculoDTO",
        description = "Ativo de frota (designacao, tipo, lotacao, proprietario, status de missao). "
                + "Payload de borda; nao e criterio de filtro. OpenAPI 3.1 e x-ui (demo).")
public class VeiculoDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Nome", controlType = FieldControlType.INPUT, required = true, maxLength = 200, icon = "badge")
    @Schema(
            description = "Designacao ou matricula conhecida da aeronave/veiculo.")
    private String nome;

    @UISchema(label = "Tipo", controlType = FieldControlType.SELECT, icon = "category")
    @Schema(
            description = "Classe de plataforma; VeiculoTipo.")
    private VeiculoTipo tipo;

    @Min(0)
    @UISchema(label = "Capacidade", type = FieldDataType.NUMBER, icon = "location_city")
    @Schema(
            description = "Lugares ou carga maxima usada no escalonamento (unidade de dominio).")
    private Integer capacidade;

    @UISchema(label = "Proprietário", controlType = FieldControlType.SELECT,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter",
            tableHidden = true, icon = "inventory_2")
    @Schema(
            description = "FK; colaborador responsavel pela custodia (proprietarioId).")
    private Integer proprietarioId;

    @UISchema(label = "Proprietário", readOnly = true, formHidden = true, icon = "inventory_2")
    @Schema(
            description = "Nome do proprietario denormalizado para tabela e HATEOAS (read model).")
    private String proprietarioNome;

    @NotNull
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, required = true, icon = "toggle_on")
    @Schema(
            description = "Elegibilidade em sorties; VeiculoStatus.")
    private VeiculoStatus status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public VeiculoTipo getTipo() { return tipo; }
    public void setTipo(VeiculoTipo tipo) { this.tipo = tipo; }
    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }
    public Integer getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(Integer proprietarioId) { this.proprietarioId = proprietarioId; }
    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String proprietarioNome) { this.proprietarioNome = proprietarioNome; }
    public VeiculoStatus getStatus() { return status; }
    public void setStatus(VeiculoStatus status) { this.status = status; }
}



