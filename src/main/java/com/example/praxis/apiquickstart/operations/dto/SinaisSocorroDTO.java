package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.SinalSocorroStatus;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@Schema(
        name = "SinaisSocorroDTO",
        description = "Chamado de emergencia (origem, teatro, escala de ameaca, abertura e encerramento). "
                + "Representa a entrada operacional de triagem para priorizar resposta, acompanhar status e medir tempo de atendimento.")
public class SinaisSocorroDTO {
    @Schema(description = "Identificador interno (PK) deste registo no servico; referencia o recurso em URLs e relacionamentos.")
    private Integer id;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Origem", type = FieldDataType.TEXT, required = true, maxLength = 200, group = "Principal", order = 10, icon = "source")
    @Schema(
            description = "Entidade que abriu o sinal (cidade, asset, unidade de campo).")
    private String origem;

    @Size(max = 200)
    @UISchema(label = "Local", type = FieldDataType.TEXT, controlType = FieldControlType.INPUT, maxLength = 200, group = "Principal", order = 20, icon = "location_on")
    @Schema(
            description = "Ponto de gravidade ou endereco aproximado do incidente.")
    private String local;

    @Min(0)
    @UISchema(label = "Nível de Ameaça", type = FieldDataType.NUMBER, group = "Principal", order = 30, icon = "warning")
    @Schema(
            description = "Escala tatica 0+; reforco de prioridade de QRF.")
    private Integer nivelAmeaca;

    @NotNull
    @UISchema(label = "Status", controlType = FieldControlType.SELECT, group = "Principal", order = 40, icon = "toggle_on")
    @Schema(
            description = "Ciclo do chamado; SinalSocorroStatus.")
    private SinalSocorroStatus status;

    @UISchema(label = "Aberto em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, group = "Datas", order = 10, icon = "event")
    @Schema(
            description = "Abertura do sinal; ancora a linha de tempo de resposta.")
    private OffsetDateTime abertoEm;

    @UISchema(label = "Fechado em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, group = "Datas", order = 20, icon = "event")
    @Schema(
            description = "Encerramento; nulo se ainda ativo na central.")
    private OffsetDateTime fechadoEm;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public Integer getNivelAmeaca() { return nivelAmeaca; }
    public void setNivelAmeaca(Integer nivelAmeaca) { this.nivelAmeaca = nivelAmeaca; }
    public SinalSocorroStatus getStatus() { return status; }
    public void setStatus(SinalSocorroStatus status) { this.status = status; }
    public OffsetDateTime getAbertoEm() { return abertoEm; }
    public void setAbertoEm(OffsetDateTime abertoEm) { this.abertoEm = abertoEm; }
    public OffsetDateTime getFechadoEm() { return fechadoEm; }
    public void setFechadoEm(OffsetDateTime fechadoEm) { this.fechadoEm = fechadoEm; }
}


