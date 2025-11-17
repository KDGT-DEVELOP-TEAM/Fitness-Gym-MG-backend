package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PostureImagePositionConverter implements AttributeConverter<PostureImagePosition, String> {

    @Override
    public String convertToDatabaseColumn(PostureImagePosition attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public PostureImagePosition convertToEntityAttribute(String dbData) {
        return dbData != null ? PostureImagePosition.fromCode(dbData) : null;
    }
}


