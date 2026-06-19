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
                + "Usado para montar recortes do catalogo de risco sem alterar o cadastro mestre de ameacas.")
public class AmeacaFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome da ameaça", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Busca por nome, codinome ou organização registrada no catálogo de ameaças.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do codinome, organizacao ou designacao publica da ameaca; aplica busca parcial no titulo operacional.")
    private String nome;

    @UISchema(label = "Classe da ameaça", controlType = FieldControlType.SELECT, order = 20,
            helpText = "Restringe a busca à categoria operacional da ameaça.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Classe taxonomica da ameaca, restringindo o resultado a um valor do catalogo AmeacaClasse.")
    private AmeacaClasse classe;

    @UISchema(label = "Planeta ou setor", controlType = FieldControlType.INPUT, maxLength = 120, order = 30,
            helpText = "Filtra pelo planeta, setor ou região de atuação conhecida.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Planeta, regiao ou setor principal de atuacao; permite busca textual parcial no teatro operacional catalogado.")
    private String planeta;

    @UISchema(label = "Nível de risco", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 40,
            helpText = "Define uma faixa de criticidade para priorizar ameaças mais ou menos perigosas.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "nivel")
    @Schema(
            description = "Faixa de nivel de perigo usada para priorizar vigilancia, resposta e comparacao entre ameacas.")
    private List<Integer> nivelBetween;

    @UISchema(label = "Status da ameaça", controlType = FieldControlType.SELECT, order = 50,
            helpText = "Mostra ameaças conforme a situação operacional atual.", icon = "toggle_on")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Status operacional da ameaca no catalogo, usado para separar riscos ativos, neutralizados ou em vigilancia.")
    private AmeacaStatus status;

    @UISchema(label = "Faixa de recompensa", type = FieldDataType.NUMBER, controlType = FieldControlType.PRICE_RANGE, order = 60,
            helpText = "Filtra pelo valor de recompensa associado à captura ou neutralização.",
            numericFormat = NumericFormat.CURRENCY, numericStep = "0.01", icon = "payments")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "recompensa")
    @Schema(
            description = "Faixa de recompensa associada a captura ou inteligencia, expressa na moeda de referencia do backend.")
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
