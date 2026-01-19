package com.example.fitnessgym_mg.entity.enums;

/**
 * 性別Enum
 * 顧客の性別を表す
 */
public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    /**
     * 表示用ラベルを取得
     * 
     * @return ラベル（"男"または"女"）
     */
    public String getLabel() {
        return label;
    }

    /**
     * コード値を取得
     * 
     * @return コード値（"male"または"female"）
     */
    public String getCode() {
        return this.name().toLowerCase();
    }

    /**
     * コード値からEnumを取得
     * 
     * @param value コード値
     * @return Gender Enum
     * @throws IllegalArgumentException valueがnull、または有効なコード値でない場合
     */
    public static Gender fromCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Gender code cannot be null");
        }
        try {
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Gender code: " + value, e);
        }
    }

    /**
     * 文字列値からEnumを取得（ENUM名または日本語ラベルをサポート）
     * 
     * <p>後方互換性のため、ENUM名（"MALE"、"FEMALE"）と日本語ラベル（"男"、"女"）の両方をサポートします。</p>
     * 
     * @param value 文字列値（ENUM名または日本語ラベル）
     * @return Gender Enum
     * @throws IllegalArgumentException valueがnull、または有効な値でない場合
     */
    public static Gender fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Gender value cannot be null");
        }
        // まずENUM名として試行
        try {
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // ENUM名でない場合、日本語ラベルから検索（既存データの互換性のため）
            for (Gender gender : Gender.values()) {
                if (gender.getLabel().equals(value)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("Invalid gender value: " + value, e);
        }
    }
}


