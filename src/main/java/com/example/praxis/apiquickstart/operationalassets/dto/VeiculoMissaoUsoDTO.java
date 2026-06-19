package com.example.praxis.apiquickstart.operationalassets.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "VeiculoMissaoUsoDTO",
        description = "Registro de uso de frota em uma missao, vinculando veiculo, missao, piloto, partida, chegada e observacoes operacionais da sortie.")
public class VeiculoMissaoUsoDTO {
    @Schema(description = "Identificador do uso de veiculo na missao; referencia a sortie em URLs, auditoria operacional e relacionamentos de frota.")
    private Integer id;

    @NotNull
    @UISchema(label = "Veículo", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Assets.VEICULOS_VEHICLE_LOOKUP_OPTIONS, required = true,
            tableHidden = true, icon = "directions_car")
    @Schema(
            description = "FK; ativo de frota utilizado (veiculoId).")
    private Integer veiculoId;

    @NotNull
    @UISchema(label = "Missão", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
        endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.Operations.MISSOES_MISSION_LOOKUP_OPTIONS, required = true,
            tableHidden = true, icon = "flag")
    @Schema(
            description = "FK; operacao a que a sortie pertence (missaoId).")
    private Integer missaoId;

    @UISchema(label = "Piloto", controlType = FieldControlType.ENTITY_LOOKUP,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS,
            tableHidden = true, icon = "flag")
    @Schema(
            description = "FK; piloto responsavel (pilotoId).")
    private Integer pilotoId;

    @UISchema(label = "Veículo", readOnly = true, formHidden = true, icon = "directions_car")
    @Schema(
            description = "Designacao do veiculo denormalizada para listagem (read model).")
    private String veiculoNome;

    @UISchema(label = "Missão", readOnly = true, formHidden = true, icon = "flag")
    @Schema(
            description = "Titulo da missao denormalizado para listagem (read model).")
    private String missaoTitulo;

    @UISchema(label = "Piloto", readOnly = true, formHidden = true, icon = "flag")
    @Schema(
            description = "Nome do piloto denormalizado (read model).")
    private String pilotoNome;

    @UISchema(label = "Partida", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "flag")
    @Schema(
            description = "Instante de partida/ decolagem (fuso ancorado no registo).")
    private OffsetDateTime partida;

    @UISchema(label = "Chegada", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "flag")
    @Schema(
            description = "Instante de retorno/ pouso; nulo se sortie ainda ativa.")
    private OffsetDateTime chegada;

    @Size(max = 2000)
    @UISchema(label = "Observação", controlType = FieldControlType.TEXTAREA, maxLength = 2000, icon = "notes")
    @Schema(
            description = "Notas tecnicas, danos ou consumiveis (texto livre).")
    private String observacao;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Integer veiculoId) { this.veiculoId = veiculoId; }
    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public Integer getPilotoId() { return pilotoId; }
    public void setPilotoId(Integer pilotoId) { this.pilotoId = pilotoId; }
    public String getVeiculoNome() { return veiculoNome; }
    public void setVeiculoNome(String veiculoNome) { this.veiculoNome = veiculoNome; }
    public String getMissaoTitulo() { return missaoTitulo; }
    public void setMissaoTitulo(String missaoTitulo) { this.missaoTitulo = missaoTitulo; }
    public String getPilotoNome() { return pilotoNome; }
    public void setPilotoNome(String pilotoNome) { this.pilotoNome = pilotoNome; }
    public OffsetDateTime getPartida() { return partida; }
    public void setPartida(OffsetDateTime partida) { this.partida = partida; }
    public OffsetDateTime getChegada() { return chegada; }
    public void setChegada(OffsetDateTime chegada) { this.chegada = chegada; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}


