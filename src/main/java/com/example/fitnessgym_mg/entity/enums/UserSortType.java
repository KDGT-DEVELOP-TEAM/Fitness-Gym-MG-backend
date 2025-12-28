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
     * @return UserSortType
     * @throws IllegalArgumentException codeがnull、空文字列、または有効なコード値でない場合
     */
    public static UserSortType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("UserSortType code cannot be null or empty");
        }
        for (UserSortType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid UserSortType code: " + code);
    }
}

