package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.hr.enums.Sentimento;
import jakarta.validation.constraints.*;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.time.OffsetDateTime;

/**
 * DTO da mencao de midia associada ao funcionario.
 *
 * <p>Representa evidencias externas que alimentam reputacao e leitura publica
 * do colaborador: veiculo, sentimento, link e instante de publicacao.
 */
@Schema(
        name = "MencoesMidiaDTO",
        description = "Evidencia externa (noticia, blog, rede) que cita o colaborador ou o alter ego: alimenta o score publico e trilhas de auditoria de imagem. "
                + "Cada registo e uma mencao pontual; sentimento classifica impacto qualitativo (NEG, NEU, POS) para agregacoes.")
public class MencoesMidiaDTO {
    @Schema(
            description = "Chave da mencao. Historico imutavel em sentido lato: correcoes geram novo registo ou fluxo de retificacao fora deste DTO (demo).",
            example = "1")
    private Integer id;

    @UISchema(label = "Funcionário", controlType = FieldControlType.SELECT,
            valueField = "id", displayField = "label",
            endpoint = com.example.praxis.apiquickstart.constants.ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter",
            tableHidden = true, helpText = "Colaborador mencionado na mídia.", icon = "badge")
    @Schema(
            description = "Herói citado na peca de midia; FK ao Funcionario. Usado para cruzar com Reputacao e perfil publico.",
            example = "2")
    private Integer funcionarioId;

    @UISchema(label = "Funcionário", readOnly = true, formHidden = true, helpText = "Nome do colaborador (preenchido automaticamente).", icon = "badge")
    @Schema(description = "Nome civil exibido em tabelas de monitorizacao; combinar com IdentidadeSecreta se a peca citar o codinome (regra fora do DTO).")
    private String funcionarioNome;

    @NotBlank
    @Size(max = 200)
    @UISchema(label = "Veículo", controlType = FieldControlType.INPUT, required = true, maxLength = 200, helpText = "Canal de publicação (ex: Jornal, Blog).", icon = "directions_car")
    @Schema(
            description = "Outlet ou canal (jornal, portal, programa) onde a referencia foi publicada; nao e o URL completo (veja url).",
            example = "Metropolis Times")
    private String veiculo;

    @UISchema(label = "Sentimento", controlType = FieldControlType.SELECT, helpText = "Classificação da menção (Positiva, Negativa, Neutra).", icon = "mood")
    @Schema(
            description = "Classificacao automatica ou curada: NEG (crise), NEU, POS. Alimenta agregacoes e pode disparar governanca de resposta (demo).")
    private Sentimento sentimento;

    @Size(max = 500)
    @UISchema(label = "URL", type = FieldDataType.URL, maxLength = 500, helpText = "Link para a matéria original.", icon = "link")
    @Schema(
            description = "Ligacao canonica a materia original para verificacao e direito de resposta; tratar conteudo externo (LGPD, copyright) fora do armazenamento de texto longo.",
            example = "https://midia.exemplo.com/nota/2025/hero-ops")
    private String url;

    @UISchema(label = "Publicado Em", type = FieldDataType.DATE, controlType = FieldControlType.DATE_TIME_PICKER, helpText = "Data e hora da publicação da menção.", icon = "event")
    @Schema(
            description = "Instante de publicacao referido na fonte; linha de tempo de reputacao e janela de reaccao (demo).",
            example = "2025-03-10T08:00:00Z")
    private OffsetDateTime publicadoEm;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public void setFuncionarioNome(String funcionarioNome) { this.funcionarioNome = funcionarioNome; }
    public String getVeiculo() { return veiculo; }
    public void setVeiculo(String veiculo) { this.veiculo = veiculo; }
    public Sentimento getSentimento() { return sentimento; }
    public void setSentimento(Sentimento sentimento) { this.sentimento = sentimento; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public OffsetDateTime getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(OffsetDateTime publicadoEm) { this.publicadoEm = publicadoEm; }
}
