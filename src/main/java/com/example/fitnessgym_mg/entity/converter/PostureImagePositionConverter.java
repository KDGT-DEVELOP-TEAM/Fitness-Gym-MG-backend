package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PostureImagePositionConverter implements AttributeConverter<PostureImagePosition, String> {

    @Override
    public String convertToDatabaseColumn(PostureImagePosition attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public PostureImagePosition convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        PostureImagePosition position = PostureImagePosition.fromCode(dbData);
        if (position == null) {
            throw new IllegalArgumentException("Invalid PostureImagePosition code: " + dbData);
        }
        return position;
    }
}


