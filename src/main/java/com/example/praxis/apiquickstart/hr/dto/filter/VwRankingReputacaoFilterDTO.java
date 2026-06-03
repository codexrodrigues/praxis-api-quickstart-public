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

@Schema(
        name = "VwRankingReputacaoFilterDTO",
        description = "Criterios de busca sobre a vista VwRankingReputacao (linha de ranking, nao entidade editavel). "
                + "Filtra colunas desnormalizadas de score, media e posicao; distinto de ReputacaoFilterDTO (tabela base). GenericFilter / POST /filter (demo).")
public class VwRankingReputacaoFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Colaborador / Herói", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 10,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Destacar posição de um colaborador no ranking.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Foco em um unico colaborador no ranking; EQUAL por funcionarioId (demo).")
    private Integer funcionarioId;

    @UISchema(label = "Funcionários (Incluir)", type = FieldDataType.NUMBER, controlType = FieldControlType.ASYNC_SELECT, order = 15,
            valueField = "id", displayField = "label",
            endpoint = ApiPaths.HumanResources.FUNCIONARIOS + "/options/filter", helpText = "Comparar posições de múltiplos colaboradores.", icon = "checklist")
    @Filterable(operation = Filterable.FilterOperation.IN, relation = "funcionarioId")
    @Schema(
            description = "Subconjunto de candidatos; operacao IN na coluna desnormalizada (multi-select) (demo).")
    private List<Integer> funcionarioIdsIn;

    @UISchema(label = "Nome Completo", controlType = FieldControlType.INPUT, maxLength = 200, order = 20, helpText = "Procurar colaborador no ranking pelo nome.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Pesquisa em nome civil exibido na tabela; LIKE (demo).")
    private String nomeCompleto;

    @UISchema(label = "Codinome", controlType = FieldControlType.INPUT, maxLength = 120, order = 30, helpText = "Procurar no ranking por nome de guerra.", icon = "theater_comedy")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Filtro por alter ego/ codinome; LIKE (demo).")
    private String codinome;

    @UISchema(label = "Equipe", controlType = FieldControlType.INPUT, maxLength = 120, order = 40, helpText = "Filtrar o ranking para exibir apenas uma equipe.", icon = "groups")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Rotulo de equipa na linha; LIKE (texto, nao FK) (demo).")
    private String equipe;

    @UISchema(label = "Score de Mídia", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 50, helpText = "Restringir ranking pela aprovação pública.", icon = "campaign")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scorePublico")
    @Schema(
            description = "Faixa de coluna scorePublico; BETWEEN (ranking) (demo).")
    private List<Integer> scorePublicoBetween;

    @UISchema(label = "Score Governamental", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 60, helpText = "Restringir ranking pelo alinhamento do governo.", icon = "gavel")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "scoreGovernamental")
    @Schema(
            description = "Faixa de score governamental; BETWEEN (demo).")
    private List<Integer> scoreGovernamentalBetween;

    @UISchema(label = "Média Final", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 70, helpText = "Filtrar ranking por faixas de média final.", icon = "analytics")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "media")
    @Schema(
            description = "Faixa da metrica de ordenacao/ destaque; BETWEEN (regra de agregacao na vista) (demo).")
    private List<BigDecimal> mediaBetween;

    @UISchema(label = "Posição no Ranking", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 80, helpText = "Exibir apenas heróis dentro de faixas de posições (ex: Top 10).", icon = "analytics")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "posicao")
    @Schema(
            description = "Fatiar o ranking top-N ou intervalo de posicoes; BETWEEN (ordinais) (demo).")
    private List<Long> posicaoBetween;

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }
    public List<Integer> getFuncionarioIdsIn() { return funcionarioIdsIn; }
    public void setFuncionarioIdsIn(List<Integer> funcionarioIdsIn) { this.funcionarioIdsIn = funcionarioIdsIn; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getCodinome() { return codinome; }
    public void setCodinome(String codinome) { this.codinome = codinome; }
    public String getEquipe() { return equipe; }
    public void setEquipe(String equipe) { this.equipe = equipe; }
    public List<Integer> getScorePublicoBetween() { return scorePublicoBetween; }
    public void setScorePublicoBetween(List<Integer> scorePublicoBetween) { this.scorePublicoBetween = scorePublicoBetween; }
    public List<Integer> getScoreGovernamentalBetween() { return scoreGovernamentalBetween; }
    public void setScoreGovernamentalBetween(List<Integer> scoreGovernamentalBetween) { this.scoreGovernamentalBetween = scoreGovernamentalBetween; }
    public List<BigDecimal> getMediaBetween() { return mediaBetween; }
    public void setMediaBetween(List<BigDecimal> mediaBetween) { this.mediaBetween = mediaBetween; }
    public List<Long> getPosicaoBetween() { return posicaoBetween; }
    public void setPosicaoBetween(List<Long> posicaoBetween) { this.posicaoBetween = posicaoBetween; }
}
