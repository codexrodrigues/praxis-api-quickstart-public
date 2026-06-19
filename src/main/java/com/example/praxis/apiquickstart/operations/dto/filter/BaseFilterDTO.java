package com.example.praxis.apiquickstart.operations.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.praxis.apiquickstart.operations.enums.BaseSigilo;
import com.example.praxis.apiquickstart.operations.enums.BaseTipo;
import org.praxisplatform.uischema.FieldControlType;
import org.praxisplatform.uischema.extension.annotation.UISchema;
import org.praxisplatform.uischema.filter.annotation.Filterable;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;

@Schema(
        name = "BaseFilterDTO",
        description = "Criterios de busca em bases e instalacoes operacionais. "
                + "Apoia descoberta por nome, categoria, nivel de sigilo e localizacao regional ou planetaria.")
public class BaseFilterDTO implements GenericFilterDTO {
    @UISchema(label = "Nome da base", controlType = FieldControlType.INPUT, maxLength = 200, order = 10,
            helpText = "Busca pelo nome operacional da base ou instalação.", icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do nome operacional usado para identificar a base ou instalacao.")
    private String nome;

    @UISchema(label = "Tipo de base", controlType = FieldControlType.SELECT, order = 20,
            helpText = "Seleciona a categoria da instalação operacional.", icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Categoria da instalacao operacional, como quartel, hangar, laboratorio ou base avancada.")
    private BaseTipo tipo;

    @UISchema(label = "Nível de sigilo", controlType = FieldControlType.SELECT, order = 30,
            helpText = "Filtra bases conforme classificação de acesso.", icon = "security")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Classificacao de sigilo que governa visibilidade e acesso a base.")
    private BaseSigilo sigilo;

    @UISchema(label = "Planeta ou região", controlType = FieldControlType.INPUT, maxLength = 120, order = 40,
            helpText = "Busca pelo planeta, setor ou região onde a base está localizada.", icon = "public")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Trecho do planeta, setor ou regiao onde a base esta localizada.")
    private String planeta;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BaseTipo getTipo() { return tipo; }
    public void setTipo(BaseTipo tipo) { this.tipo = tipo; }
    public BaseSigilo getSigilo() { return sigilo; }
    public void setSigilo(BaseSigilo sigilo) { this.sigilo = sigilo; }
    public String getPlaneta() { return planeta; }
    public void setPlaneta(String planeta) { this.planeta = planeta; }
}


