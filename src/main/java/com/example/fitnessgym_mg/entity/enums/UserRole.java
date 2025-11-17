package com.example.fitnessgym_mg.entity.enums;

public enum UserRole {
    ADMIN,
    MANAGER,
    TRAINER;

    public static UserRole fromCode(String value) {
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.toUpperCase());
    }
}


