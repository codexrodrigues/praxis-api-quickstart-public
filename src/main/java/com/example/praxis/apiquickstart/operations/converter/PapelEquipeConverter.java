package com.example.praxis.apiquickstart.operations.converter;

import com.example.praxis.apiquickstart.operations.enums.PapelEquipe;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PapelEquipeConverter implements AttributeConverter<PapelEquipe, String> {
    @Override
    public String convertToDatabaseColumn(PapelEquipe attribute) {
        if (attribute == null) return null;
        if (attribute == PapelEquipe.CO_LIDER) return "CO-LIDER";
        return attribute.name();
    }

    @Override
    public PapelEquipe convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if ("CO-LIDER".equalsIgnoreCase(dbData)) return PapelEquipe.CO_LIDER;
        return PapelEquipe.valueOf(dbData.toUpperCase());
    }
}



