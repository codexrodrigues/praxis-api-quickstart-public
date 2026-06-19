package com.example.praxis.apiquickstart.hr.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;

import java.math.BigDecimal;

@UISchema(label = "Perfil do Herói", readOnly = true, icon = "account_circle")
@Schema(
        name = "VwPerfilHeroiDTO",
        description = "Projecao somente leitura que consolida cadastro civil, identidade operacional, reputacao, habilidades resumidas, equipe e base principal. "
                + "Serve a ficha 360, catalogo semantico e assistentes LLM sem substituir os contratos transacionais de cadastro.")
public class VwPerfilHeroiDTO {
    @Schema(
            description = "Chave do funcionario; ancora o conjunto de atributos exibidos na ficha de perfil.",
            example = "3")
    @UISchema(label = "Cód. Herói", helpText = "Identificador do herói neste perfil.", formHidden = true, icon = "badge")
    private Integer funcionarioId;

    @UISchema(
            label = "Foto",
            type = FieldDataType.URL,
            controlType = FieldControlType.AVATAR,
            helpText = "Foto de perfil usada para reconhecer visualmente o funcionário nesta ficha 360.",
            icon = "account_circle",
            order = 5)
    @Schema(description = "URL da foto de perfil do funcionário, reutilizada da ficha cadastral para identificar visualmente a projeção 360.")
    private String avatarUrl;

    @UISchema(label = "Nome Completo", helpText = "Nome civil do herói.", icon = "badge")
    @Schema(description = "Nome civil oficial na folha e contrato.")
    private String nomeCompleto;

    @UISchema(label = "Codinome", helpText = "Identidade pública (alter ego).", icon = "theater_comedy")
    @Schema(description = "Codinome de identidade secreta ou marca publica; vazio se inexistente.")
    private String codinome;

    @UISchema(label = "Universo", helpText = "Universo narrativo de origem.", icon = "public")
    @Schema(description = "Ficcao/linha editoral a que o alter ego pertence (ex.: continuidade, quadrinho); dado de catalogo na vista.")
    private String universo;

    @UISchema(label = "Exposição Pública", type = FieldDataType.BOOLEAN, helpText = "Aprovação para exposição.", icon = "badge")
    @Schema(
            description = "Indica se o colaborador pode ser tratado como figura de exposicao publica em regras de privacidade, midia e beneficios.",
            example = "true")
    private Boolean exposicaoPublica;

    @UISchema(label = "Cargo", helpText = "Ocupação atual.", icon = "work")
    @Schema(description = "Cargo corrente ou principal exibido; texto desnormalizado vindo do join (nao e substituto de CargoDTO).")
    private String cargo;

    @UISchema(label = "Departamento", helpText = "Departamento atual.", icon = "apartment")
    @Schema(description = "Unidade organizacional de alocacao; desnormalizado para a vista.")
    private String departamento;

    @UISchema(label = "Score Público", type = FieldDataType.NUMBER, helpText = "Score de reputação pública.", icon = "work")
    @Schema(
            description = "Snapshot de reputacao publica/ midia; mesma semantica que em Reputacao, aqui congelada na projecao.",
            example = "72")
    private Integer scorePublico;

    @UISchema(label = "Score Governamental", type = FieldDataType.NUMBER, helpText = "Score de confiança governamental.", icon = "gavel")
    @Schema(
            description = "Snapshot de alinhamento governamental/compliance; par ao score publico na analise de risco de imagem.",
            example = "88")
    private Integer scoreGovernamental;

    @UISchema(label = "Score Médio", type = FieldDataType.NUMBER, helpText = "Média de reputação.", icon = "analytics")
    @Schema(
            description = "Indice agregado de reputacao usado como destaque sintetico da ficha, derivado dos scores publico e governamental.",
            example = "80.0")
    private BigDecimal scoreMedio;

    @UISchema(label = "Habilidades", helpText = "Resumo de competências cadastradas.", icon = "psychology")
    @Schema(
            description = "Lista ou texto resumido de competencias (pode ser concatenacao para exibicao; detalhe por habilidade em recursos de ligacao N-N).")
    private String habilidades;

    @UISchema(label = "Equipe Principal", helpText = "Equipe tática primária.", icon = "groups")
    @Schema(description = "Equipa tatica predominante a que o colaborador esta associado (catalogo/label na vista).")
    private String equipePrincipal;

    @UISchema(label = "Base Principal", helpText = "Base operacional de atuação.", icon = "groups")
    @Schema(description = "Base operacional principal associada ao colaborador para suporte a logistica, escala e planejamento de missao.")
    private String basePrincipal;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCodinome() { return codinome; }
    public void setCodinome(String codinome) { this.codinome = codinome; }
    public String getUniverso() { return universo; }
    public void setUniverso(String universo) { this.universo = universo; }
    public Boolean getExposicaoPublica() { return exposicaoPublica; }
    public void setExposicaoPublica(Boolean exposicaoPublica) { this.exposicaoPublica = exposicaoPublica; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public Integer getScorePublico() { return scorePublico; }
    public void setScorePublico(Integer scorePublico) { this.scorePublico = scorePublico; }
    public Integer getScoreGovernamental() { return scoreGovernamental; }
    public void setScoreGovernamental(Integer scoreGovernamental) { this.scoreGovernamental = scoreGovernamental; }
    public BigDecimal getScoreMedio() { return scoreMedio; }
    public void setScoreMedio(BigDecimal scoreMedio) { this.scoreMedio = scoreMedio; }
    public String getHabilidades() { return habilidades; }
    public void setHabilidades(String habilidades) { this.habilidades = habilidades; }
    public String getEquipePrincipal() { return equipePrincipal; }
    public void setEquipePrincipal(String equipePrincipal) { this.equipePrincipal = equipePrincipal; }
    public String getBasePrincipal() { return basePrincipal; }
    public void setBasePrincipal(String basePrincipal) { this.basePrincipal = basePrincipal; }
}
