package com.example.praxis.apiquickstart.operations.converter;

import com.example.praxis.apiquickstart.operations.enums.ResultadoMissao;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ResultadoMissaoConverter implements AttributeConverter<ResultadoMissao, String> {
    @Override
    public String convertToDatabaseColumn(ResultadoMissao attribute) {
        if (attribute == null) return null;
        if (attribute == ResultadoMissao.NA) return "N/A";
        return attribute.name();
    }

    @Override
    public ResultadoMissao convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if ("N/A".equalsIgnoreCase(dbData)) return ResultadoMissao.NA;
        return ResultadoMissao.valueOf(dbData.toUpperCase());
    }
}



