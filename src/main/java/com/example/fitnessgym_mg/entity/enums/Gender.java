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
     * @return Gender Enum、見つからない場合はnull
     */
    public static Gender fromCode(String value) {
        if (value == null) {
            return null;
        }
        return Gender.valueOf(value.toUpperCase());
    }
}


