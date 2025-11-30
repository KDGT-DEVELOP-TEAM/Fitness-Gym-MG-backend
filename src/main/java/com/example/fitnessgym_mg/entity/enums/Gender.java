package com.example.fitnessgym_mg.entity.enums;

public enum Gender {
    MALE,
    FEMALE;

    public String getCode() {
        return this.name().toLowerCase();
    }

    public static Gender fromCode(String value) {
        if (value == null) {
            return null;
        }
        return Gender.valueOf(value.toUpperCase());
    }
}


