package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("UserRole cannot be null");
        }
        return attribute.getCode();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;  // DBのnullは許容
        }
        return UserRole.fromCode(dbData);  // nullチェック不要（fromCodeが例外を投げる）
    }
}

