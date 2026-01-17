package com.example.fitnessgym_mg.entity.enums;

/**
 * パスワードリセットリクエストの状態
 */
public enum PasswordResetStatus {
    PENDING,    // 未処理
    APPROVED,   // 承認済み
    REJECTED;   // 拒否済み

    /**
     * DBコード値を取得
     * 
     * @return コード値（小文字のEnum名、例: "pending", "approved", "rejected"）
     */
    public String getCode() {
        return this.name().toLowerCase();
    }

    /**
     * コード値からEnumを取得
     * 
     * @param code コード値（小文字のEnum名）
     * @return PasswordResetStatus
     * @throws IllegalArgumentException 無効なコード値の場合
     */
    public static PasswordResetStatus fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("PasswordResetStatus code cannot be null or empty");
        }
        for (PasswordResetStatus status : values()) {
            if (status.getCode().equals(code.toLowerCase())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid PasswordResetStatus code: " + code);
    }
}
