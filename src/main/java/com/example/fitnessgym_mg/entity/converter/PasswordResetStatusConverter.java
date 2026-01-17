package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.PasswordResetStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * PasswordResetStatusのJPA AttributeConverter
 * <p>PostgreSQL ENUM型（password_reset_status）とPasswordResetStatus Enumの変換を行います。</p>
 */
@Converter
public class PasswordResetStatusConverter implements AttributeConverter<PasswordResetStatus, String> {

    @Override
    public String convertToDatabaseColumn(PasswordResetStatus attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("PasswordResetStatus cannot be null");
        }
        return attribute.getCode();
    }

    @Override
    public PasswordResetStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;  // DBのnullは許容
        }
        return PasswordResetStatus.fromCode(dbData);  // nullチェック不要（fromCodeが例外を投げる）
    }
}
