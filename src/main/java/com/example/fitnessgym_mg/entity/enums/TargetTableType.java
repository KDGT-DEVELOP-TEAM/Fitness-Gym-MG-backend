package com.example.fitnessgym_mg.entity.enums;

/**
 * 監査ログの対象テーブル種別
 */
public enum TargetTableType {
    USERS("users"),
    CUSTOMERS("customers"),
    STORES("stores"),
    LESSONS("lessons"),
    TRAININGS("trainings"),
    POSTURE_GROUPS("posture_groups"),
    POSTURE_IMAGES("posture_images"),
    LOGS("logs");

    private final String tableName;

    TargetTableType(String tableName) {
        this.tableName = tableName;
    }

    /**
     * DBのテーブル名を取得
     * 
     * @return テーブル名（小文字、スネークケース）
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * テーブル名からTargetTableTypeを取得
     * 
     * @param tableName テーブル名
     * @return TargetTableType
     * @throws IllegalArgumentException 無効なテーブル名の場合
     */
    public static TargetTableType fromTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("TargetTableType table name cannot be null or empty");
        }
        for (TargetTableType type : values()) {
            if (type.tableName.equalsIgnoreCase(tableName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TargetTableType table name: " + tableName);
    }
}

