package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.constants.ApiPaths;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtro da view agregada de perfil do heroi.
 *
 * <p>Esse contrato mistura identificacao da pessoa, atributos reputacionais
 * e dimensoes agregadas vindas de option-sources, como universo, equipe e base.
 */
@Schema(
        name = "VwPerfilHeroiFilterDTO",
        description = "Criterios de busca na vista VwPerfilHeroi (ficha agregada; nao e alteracao de cadastro so por filtrar). "
                + "Cruza identidade, universo, organograma, reputacao e contexto operacional para montar fichas agregadas de colaboradores.")
public class VwPerfilHeroiFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Herói", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Filtrar perfil de um colaborador.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Colaborador especifico cuja ficha agregada deve ser localizada.")
    private Integer funcionarioId;

    @UISchema(label = "Mostrar heróis", type = FieldDataType.NUMBER, controlType = FieldControlType.INLINE_ENTITY_LOOKUP, order = 15,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS_EMPLOYEE_LOOKUP_OPTIONS, helpText = "Inclui na ficha agregada apenas os heróis selecionados.", icon = "checklist")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "funcionarioId")
    @Schema(
            description = "Conjunto de colaboradores que devem aparecer na ficha agregada, sem alterar cadastros individuais.")
    private List<Integer> funcionarioIdsIn;

    @UISchema(label = "Nome Civil", controlType = FieldControlType.INPUT, maxLength = 200, order = 20, helpText = "Busca livre por nome civil do herói.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome civil do colaborador usado para busca textual.")
    private String nomeCompleto;

    @UISchema(label = "Codinome", controlType = FieldControlType.INPUT, maxLength = 120, order = 30, helpText = "Busca livre por nome de guerra.", icon = "theater_comedy")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do codinome, nome de guerra ou marca publica associada ao colaborador.")
    private String codinome;

    @UISchema(label = "Universo", controlType = FieldControlType.ASYNC_SELECT, maxLength = 120, order = 40,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_PERFIL_HEROI + "/option-sources/universo/options/filter", helpText = "Filtrar por universo narrativo de origem.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Universo ou contexto narrativo de origem usado como dimensao de segmentacao da ficha.")
    private String universo;

    @UISchema(label = "Exposição Pública", type = FieldDataType.BOOLEAN, controlType = FieldControlType.CHECKBOX, order = 50, helpText = "Filtrar por heróis públicos ou secretos.", icon = "visibility")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Indicador de exposicao publica planejada, separando perfis publicos de identidades protegidas.")
    private Boolean exposicaoPublica;

    @UISchema(label = "Cargo", controlType = FieldControlType.INPUT, maxLength = 120, order = 60, helpText = "Busca textual do cargo atual.", icon = "work")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do cargo atual exibido na vista agregada, usado em busca textual.")
    private String cargo;

    @UISchema(label = "Departamento", controlType = FieldControlType.INPUT, maxLength = 120, order = 70, helpText = "Busca textual do departamento alocado.", icon = "apartment")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do departamento exibido na vista agregada.")
    private String departamento;

    @UISchema(label = "Score de Mídia", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 80, helpText = "Filtrar por faixa de score de mídia.", icon = "campaign")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scorePublico")
    @Schema(
            description = "Faixa de score publico ou midiatico usada para comparar exposicao e reputacao.")
    private List<Integer> scorePublicoBetween;

    @UISchema(label = "Score Governamental", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 90, helpText = "Filtrar por faixa de aderência governamental.", icon = "gavel")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scoreGovernamental")
    @Schema(
            description = "Faixa de score governamental usada para avaliar alinhamento institucional.")
    private List<Integer> scoreGovernamentalBetween;

    @UISchema(label = "Score Médio", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 100, helpText = "Filtrar por avaliação geral média.", icon = "analytics")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scoreMedio")
    @Schema(
            description = "Faixa de media agregada de reputacao calculada pela vista.")
    private List<BigDecimal> scoreMedioBetween;

    @UISchema(label = "Habilidades", controlType = FieldControlType.INPUT, maxLength = 4000, order = 110, helpText = "Busca livre por palavras-chave nas habilidades.", icon = "psychology")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do resumo de competencias e habilidades concatenadas para busca rapida na ficha.")
    private String habilidades;

    @UISchema(label = "Equipe Principal", controlType = FieldControlType.ASYNC_SELECT, maxLength = 120, order = 120,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_PERFIL_HEROI + "/option-sources/equipePrincipal/options/filter", helpText = "Filtrar pela equipe de operação prioritária.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Equipe tatica principal associada ao colaborador na ficha agregada.")
    private String equipePrincipal;

    @UISchema(label = "Base Principal", controlType = FieldControlType.ASYNC_SELECT, maxLength = 120, order = 130,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.VW_PERFIL_HEROI + "/option-sources/basePrincipal/options/filter", helpText = "Filtrar por local base de referência.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Base operacional principal associada ao colaborador na ficha agregada.")
    private String basePrincipal;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public List<Integer> getFuncionarioIdsIn() { return funcionarioIdsIn; }
    public void setFuncionarioIdsIn(List<Integer> funcionarioIdsIn) { this.funcionarioIdsIn = funcionarioIdsIn; }
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
    public List<Integer> getScorePublicoBetween() { return scorePublicoBetween; }
    public void setScorePublicoBetween(List<Integer> scorePublicoBetween) { this.scorePublicoBetween = scorePublicoBetween; }
    public List<Integer> getScoreGovernamentalBetween() { return scoreGovernamentalBetween; }
    public void setScoreGovernamentalBetween(List<Integer> scoreGovernamentalBetween) { this.scoreGovernamentalBetween = scoreGovernamentalBetween; }
    public List<BigDecimal> getScoreMedioBetween() { return scoreMedioBetween; }
    public void setScoreMedioBetween(List<BigDecimal> scoreMedioBetween) { this.scoreMedioBetween = scoreMedioBetween; }
    public String getHabilidades() { return habilidades; }
    public void setHabilidades(String habilidades) { this.habilidades = habilidades; }
    public String getEquipePrincipal() { return equipePrincipal; }
    public void setEquipePrincipal(String equipePrincipal) { this.equipePrincipal = equipePrincipal; }
    public String getBasePrincipal() { return basePrincipal; }
    public void setBasePrincipal(String basePrincipal) { this.basePrincipal = basePrincipal; }
}
