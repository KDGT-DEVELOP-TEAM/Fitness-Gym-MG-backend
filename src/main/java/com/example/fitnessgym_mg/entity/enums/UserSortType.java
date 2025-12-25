package com.example.fitnessgym_mg.entity.enums;

/**
 * ユーザー一覧のソートタイプ
 */
public enum UserSortType {
    /**
     * 登録日時降順（デフォルト）
     */
    CREATED("created"),
    
    /**
     * カナ昇順
     */
    KANA("kana");
    
    private final String code;
    
    UserSortType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * コードからUserSortTypeを取得
     * 
     * @param code ソートタイプのコード
     * @return UserSortType（存在しない場合はCREATEDを返す）
     */
    public static UserSortType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return CREATED;
        }
        for (UserSortType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return CREATED; // デフォルト値
    }
}

