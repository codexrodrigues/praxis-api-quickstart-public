package com.example.praxis.apiquickstart.riskintelligence.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaClasse;
import com.example.praxis.apiquickstart.riskintelligence.enums.AmeacaStatus;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.NumericFormat;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.math.BigDecimal;
import java.util.List;

@Schema(
        name = "AmeacaFilterDTO",
        description = "Criterios de busca no catalogo de ameacas (villains, cenarios); nao e a ameaca persistida a editar so com filtrar. "
                + "Usado com GenericFilter / POST /filter no demo Risk intelligence (demo).")
public class AmeacaFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome de codinome, organizacao inimiga ou alvo; LIKE no titulo (demo).")
    private String nome;

    @UISchema(controlType = FieldControlType.SELECT, order = 20, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Recorte taxonomico (AmeacaClasse: cosmic, metahumano, etc.); EQUAL ao enum (demo).")
    private AmeacaClasse classe;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 120, order = 30, icon = "public")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Orbe ou setor de operacao catalogado; LIKE (ficcao) (demo).")
    private String planeta;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 40, icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "nivel")
    @Schema(
            description = "Escala de perigo/ prioridade; BETWEEN sobre coluna nivel (demo).")
    private List<Integer> nivelBetween;

    @UISchema(controlType = FieldControlType.SELECT, order = 50, icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Estado operacional (ativo, neutralizado, em vigilancia, etc.); EQUAL AmeacaStatus (demo).")
    private AmeacaStatus status;

    @UISchema(type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 60,
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "recompensa")
    @Schema(
            description = "Recompensa ofertada a captura; BETWEEN na moeda do backend; pode ser zero (demo).")
    private List<BigDecimal> recompensaBetween;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public AmeacaClasse getClasse() { return classe; }
    public void setClasse(AmeacaClasse classe) { this.classe = classe; }
    public String getPlaneta() { return planeta; }
    public void setPlaneta(String planeta) { this.planeta = planeta; }
    public List<Integer> getNivelBetween() { return nivelBetween; }
    public void setNivelBetween(List<Integer> nivelBetween) { this.nivelBetween = nivelBetween; }
    public AmeacaStatus getStatus() { return status; }
    public void setStatus(AmeacaStatus status) { this.status = status; }
    public List<BigDecimal> getRecompensaBetween() { return recompensaBetween; }
    public void setRecompensaBetween(List<BigDecimal> recompensaBetween) { this.recompensaBetween = recompensaBetween; }
}
