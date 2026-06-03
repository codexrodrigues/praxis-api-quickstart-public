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
        description = "Criterios de busca em bases/ instalacoes (nao e a base a persistir so por filtrar). "
                + "Tipo, sigilo e planeta de ficcao; GenericFilter / POST /filter (demo Operacoes).")
public class BaseFilterDTO implements GenericFilterDTO {
    @UISchema(controlType = FieldControlType.INPUT, maxLength = 200, order = 10, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Nome operacional da base; LIKE (demo).")
    private String nome;

    @UISchema(controlType = FieldControlType.SELECT, order = 20, icon = "category")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Classificacao (quartel, hangar, laboratorio, etc.); EQUAL BaseTipo (demo).")
    private BaseTipo tipo;

    @UISchema(controlType = FieldControlType.SELECT, order = 30, icon = "filter_list")
    @Filterable(operation = Filterable.FilterOperation.EQUAL)
    @Schema(
            description = "Nivel de acesso (publico, restrito, negro); EQUAL BaseSigilo (demo).")
    private BaseSigilo sigilo;

    @UISchema(controlType = FieldControlType.INPUT, maxLength = 120, order = 40, icon = "public")
    @Filterable(operation = Filterable.FilterOperation.LIKE)
    @Schema(
            description = "Orbe/ planeta simulado; LIKE (ficcao) (demo).")
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


