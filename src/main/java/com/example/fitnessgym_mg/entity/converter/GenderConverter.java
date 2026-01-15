package com.example.fitnessgym_mg.entity.converter;

import com.example.fitnessgym_mg.entity.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        // PostgreSQL ENUM型（customer_gender）はENUM名（"MALE"、"FEMALE"）を期待する
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // データベースの値（ENUM名 "MALE"、"FEMALE"）からEnumを取得
        // 後方互換性のため、日本語ラベル（「男」「女」）もサポート
        try {
            // まずENUM名として試行
            return Gender.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // ENUM名でない場合、日本語ラベルから検索（既存データの互換性のため）
            for (Gender gender : Gender.values()) {
                if (gender.getLabel().equals(dbData)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("Invalid gender value: " + dbData, e);
        }
    }
}

