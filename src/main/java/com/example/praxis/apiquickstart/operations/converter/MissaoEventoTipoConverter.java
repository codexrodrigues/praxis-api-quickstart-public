package com.example.praxis.apiquickstart.operations.converter;

import com.example.praxis.apiquickstart.operations.enums.MissaoEventoTipo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MissaoEventoTipoConverter implements AttributeConverter<MissaoEventoTipo, String> {
    @Override
    public String convertToDatabaseColumn(MissaoEventoTipo attribute) {
        if (attribute == null) return null;
        if (attribute == MissaoEventoTipo.SOS) return "S.O.S";
        return attribute.name();
    }

    @Override
    public MissaoEventoTipo convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if ("S.O.S".equalsIgnoreCase(dbData)) return MissaoEventoTipo.SOS;
        return MissaoEventoTipo.valueOf(dbData.toUpperCase());
    }
}



