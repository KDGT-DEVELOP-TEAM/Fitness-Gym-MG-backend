package com.example.fitnessgym_mg.entity.enums;

/**
 * 顧客一覧のソートキー
 */
public enum CustomerSort {
    /** かな順（昇順） */
    KANA,
    /** 作成日時順（降順） */
    CREATED;
    
    /**
     * 文字列からCustomerSortに変換
     * 
     * @param value 文字列値
     * @return CustomerSort、該当しない場合はCREATEDを返す
     */
    public static CustomerSort fromString(String value) {
        if (value == null) {
            return CREATED;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CREATED;
        }
    }
}

