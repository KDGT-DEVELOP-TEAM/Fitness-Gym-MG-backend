package com.example.fitnessgym_mg.entity.enums;

import java.util.Set;

public enum UserRole {
    ADMIN,
    MANAGER,
    TRAINER;

    private Set<UserRole> manageableRoles;

    UserRole() {
        // 初期化はstatic初期化ブロックで行う（enum定数の初期化順序の問題を回避）
    }

    static {
        // すべてのenum定数が初期化された後にSetを設定
        ADMIN.manageableRoles = Set.of(ADMIN, MANAGER, TRAINER);
        MANAGER.manageableRoles = Set.of(TRAINER);
        TRAINER.manageableRoles = Set.of();
    }

    /**
     * 指定されたロールのユーザーを管理できるか確認
     * 
     * <p>ビジネスルール:</p>
     * <ul>
     *   <li>ADMIN: すべてのロールを管理可能</li>
     *   <li>MANAGER: TRAINERのみ管理可能</li>
     *   <li>TRAINER: 管理不可</li>
     * </ul>
     * 
     * @param target 管理対象のロール
     * @return 管理可能な場合 true
     */
    public boolean canManage(UserRole target) {
        return manageableRoles.contains(target);
    }

    /**
     * DBコード値を取得
     * 
     * @return コード値（小文字のEnum名、例: "admin", "manager", "trainer"）
     */
    public String getCode() {
        return this.name().toLowerCase();
    }

    /**
     * コード値からEnumを取得
     * 
     * @param value コード値
     * @return UserRole Enum
     * @throws IllegalArgumentException valueがnull、または有効なコード値でない場合
     */
    public static UserRole fromCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("UserRole code cannot be null");
        }
        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UserRole code: " + value, e);
        }
    }
}


