package com.example.fitnessgym_mg.entity.enums;

import java.util.Locale;

public enum PostureImagePosition {
    FRONT("front"),
    RIGHT("right"),
    BACK("back"),
    LEFT("left");

    private final String code;

    PostureImagePosition(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PostureImagePosition fromCode(String value) {
        if (value == null) {
            return null;
        }
        for (PostureImagePosition position : values()) {
            if (position.code.equalsIgnoreCase(value)) {
                return position;
            }
        }
        throw new IllegalArgumentException("Unknown posture image position: " + value);
    }

    @Override
    public String toString() {
        return code;
    }
}


