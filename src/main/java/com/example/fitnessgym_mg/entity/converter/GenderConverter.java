package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        return attribute != null ? attribute.name().toLowerCase() : null;
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        return Gender.fromCode(dbData);
    }
}

