package com.example.praxis.apiquickstart.operations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

@UISchema(label = "Resumo de Missões", readOnly = true, icon = "flag")
@Schema(
        name = "VwResumoMissoeDTO",
        description = "Linha de vista so-leitura: resumo agregado de missao (titulo, risco, recursos, linha de tempo de acoes). "
                + "Nao e a Missao a editar; serve torre de controlo, BI e descricao semantica (demo Operacoes).")
public class VwResumoMissoeDTO {
    @Schema(
            description = "Chave da missao na origem; ancora o restante dominio (participantes, eventos) (demo).",
            example = "12")
    private Integer missaoId;

    @UISchema(label = "Título", icon = "title")
    @Schema(
            description = "Nome de missao exibido em quadros; desnormalizado (demo).")
    private String titulo;

    @UISchema(label = "Status", icon = "toggle_on")
    @Schema(
            description = "Estado de workflow (aberta, em curso, concluida, cancelada, etc.); string da vista (demo).")
    private String status;

    @UISchema(label = "Prioridade", icon = "priority_high")
    @Schema(
            description = "Nivel de escalacao/ urgencia (P1, critica, etc.); rotulo (demo).")
    private String prioridade;

    @UISchema(label = "Local", icon = "location_on")
    @Schema(
            description = "Cenario operacional (cidade, setor, coordenada textual) (demo).")
    private String local;

    @UISchema(label = "Ameaça", icon = "warning")
    @Schema(
            description = "Ameaca principal associada; texto denormalizado (pode vazio) (demo).")
    private String ameaca;

    @UISchema(label = "Qtd. Heróis", type = FieldDataType.NUMBER, icon = "badge")
    @Schema(
            description = "Contagem de herois alocados/ escalados; agregado da query (demo).",
            example = "4")
    private Long qtdHerois;

    @UISchema(label = "Qtd. Eventos", type = FieldDataType.NUMBER, icon = "event_note")
    @Schema(
            description = "Contagem de eventos de missao; proxy de atividade/ complexidade (demo).",
            example = "18")
    private Long qtdEventos;

    @UISchema(label = "Primeira Ação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "label")
    @Schema(
            description = "Instante (offset) do primeiro registo de acao/ timeline da missao (demo).")
    private OffsetDateTime primeiraAcao;

    @UISchema(label = "Última Ação", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, icon = "label")
    @Schema(
            description = "Ultimo marco conhecido; usado com primeira para duração/ staleness (demo).")
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
