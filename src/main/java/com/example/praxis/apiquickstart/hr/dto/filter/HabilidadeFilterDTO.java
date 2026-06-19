package com.example.praxis.apiquickstart.hr.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.hr.enums.HabilidadeCategoria;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.FieldDataType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

import java.util.List;

@Schema(
        name = "HabilidadeFilterDTO",
        description = "Criterios de busca no catalogo de competencias/ poderes (nao e a entidade Habilidade em edicao). "
                + "Apoia selecao de talentos por nome, categoria, intensidade operacional e descricao da capacidade.")
public class HabilidadeFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome da Habilidade", controlType = FieldControlType.INPUT, maxLength = 200, order = 10, helpText = "Filtrar habilidade por nome.", icon = "badge")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome de habilidade (ex.: voo, telepatia); LIKE sobre o campo nome.")
    private String nome;

    @UISchema(label = "Categoria", controlType = FieldControlType.SELECT, order = 20, helpText = "Filtrar por categoria da habilidade.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Recorte taxonomico; EQUAL ao enumerado HabilidadeCategoria (fisica, mental, etc., conforme enum).")
    private HabilidadeCategoria categoria;

    @UISchema(label = "Nível de Poder", type = FieldDataType.NUMBER, controlType = FieldControlType.RANGE_SLIDER, order = 30, helpText = "Buscar por faixa de nível de poder.", icon = "trending_up")
    @Filterable(operation = Filterable.FilterOperation.BETWEEN, relation = "nivelPoder")
    @Schema(
            description = "Intervalo de intensidade da habilidade, usado para compatibilizar demanda operacional, risco e perfil do colaborador.")
    private List<Integer> nivelPoderBetween;

    @UISchema(label = "Descrição", controlType = FieldControlType.INPUT, maxLength = 2000, order = 40, helpText = "Buscar palavras na descrição.", icon = "description")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Texto longo de descricao e efeitos; LIKE para procurar por palavra-chave no detalhe da habilidade.")
    private String descricao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public HabilidadeCategoria getCategoria() { return categoria; }
    public void setCategoria(HabilidadeCategoria categoria) { this.categoria = categoria; }
    public List<Integer> getNivelPoderBetween() { return nivelPoderBetween; }
    public void setNivelPoderBetween(List<Integer> nivelPoderBetween) { this.nivelPoderBetween = nivelPoderBetween; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
