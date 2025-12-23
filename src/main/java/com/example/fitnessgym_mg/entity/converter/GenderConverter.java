package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        return attribute != null ? attribute.getLabel() : null;
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // データベースの値（「男」「女」）からEnumを取得
        for (Gender gender : Gender.values()) {
            if (gender.getLabel().equals(dbData)) {
                return gender;
            }
        }
        return null;
    }
}

