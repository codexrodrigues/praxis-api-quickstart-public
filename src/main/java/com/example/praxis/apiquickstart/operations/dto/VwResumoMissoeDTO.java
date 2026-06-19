package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@UISchema(label = "Resumo de Missões", readOnly = true, icon = "flag")
@Schema(
        name = "VwResumoMissoeDTO",
        description = "Projecao somente leitura que resume missoes por estado operacional, prioridade, local, ameaca, participantes e atividade recente. "
                + "Serve a paineis de controle, catalogo semantico e assistentes LLM sem substituir o contrato transacional de Missao.")
public class VwResumoMissoeDTO {
    @Schema(
            description = "Chave da missao de origem usada para relacionar participantes, eventos e demais detalhes operacionais.",
            example = "12")
    private Integer missaoId;

    @UISchema(label = "Título", icon = "title")
    @Schema(
            description = "Titulo operacional da missao exibido em paineis e listagens de resumo.")
    private String titulo;

    @UISchema(label = "Status", icon = "toggle_on")
    @Schema(
            description = "Estado de workflow da missao projetado na vista de resumo.")
    private String status;

    @UISchema(label = "Prioridade", icon = "priority_high")
    @Schema(
            description = "Nivel de escalacao ou urgencia atribuido a missao.")
    private String prioridade;

    @UISchema(label = "Local", icon = "location_on")
    @Schema(
            description = "Local, setor ou cenario operacional principal da missao.")
    private String local;

    @UISchema(label = "Ameaça", icon = "warning")
    @Schema(
            description = "Ameaca principal associada a missao, projetada como texto de contexto operacional.")
    private String ameaca;

    @UISchema(label = "Qtd. Heróis", type = FieldDataType.NUMBER, icon = "badge")
    @Schema(
            description = "Quantidade de participantes alocados ou escalados para a missao.",
            example = "4")
    private Long qtdHerois;

    @UISchema(label = "Qtd. Eventos", type = FieldDataType.NUMBER, icon = "event_note")
    @Schema(
            description = "Quantidade de eventos registrados na linha do tempo da missao, usada como sinal de atividade e complexidade.",
            example = "18")
    private Long qtdEventos;

    @UISchema(label = "Primeira Ação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "label")
    @Schema(
            description = "Instante da primeira acao registrada na linha do tempo da missao.")
    private OffsetDateTime primeiraAcao;

    @UISchema(label = "Última Ação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "label")
    @Schema(
            description = "Instante da atividade mais recente registrada para a missao, usado para avaliar recencia operacional.")
    private OffsetDateTime ultimaAcao;

    public Integer getMissaoId() { return missaoId; }
    public void setMissaoId(Integer missaoId) { this.missaoId = missaoId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getAmeaca() { return ameaca; }
    public void setAmeaca(String ameaca) { this.ameaca = ameaca; }
    public Long getQtdHerois() { return qtdHerois; }
    public void setQtdHerois(Long qtdHerois) { this.qtdHerois = qtdHerois; }
    public Long getQtdEventos() { return qtdEventos; }
    public void setQtdEventos(Long qtdEventos) { this.qtdEventos = qtdEventos; }
    public OffsetDateTime getPrimeiraAcao() { return primeiraAcao; }
    public void setPrimeiraAcao(OffsetDateTime primeiraAcao) { this.primeiraAcao = primeiraAcao; }
    public OffsetDateTime getUltimaAcao() { return ultimaAcao; }
    public void setUltimaAcao(OffsetDateTime ultimaAcao) { this.ultimaAcao = ultimaAcao; }
}
