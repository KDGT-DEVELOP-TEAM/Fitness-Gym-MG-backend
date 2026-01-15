package com.example.fitnessgym_mg.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BMI計算ユーティリティクラス
 * 
 * <p>BMI（Body Mass Index）を計算するためのユーティリティです。</p>
 * <p>計算式: BMI = 体重(kg) / (身長(m))²</p>
 * <p>身長はcm単位で受け取り、内部でm単位に変換します。</p>
 * 
 * <p>このクラスは静的メソッドのみを提供するユーティリティクラスです。
 * インスタンス化を防ぐため、finalクラスとして定義されています。</p>
 */
public final class BmiCalculator {
    
    private BmiCalculator() {
        // インスタンス化を防ぐ
    }
    
    /**
     * cmからmへの変換係数
     */
    private static final BigDecimal CM_TO_METER = new BigDecimal("100");
    
    /**
     * BMIを計算する
     * 
     * <p>体重と身長からBMIを計算します。</p>
     * <p>計算結果は小数点以下2桁で四捨五入されます。</p>
     * 
     * @param weight 体重（kg）
     * @param height 身長（cm）
     * @return BMI値（小数点以下2桁）。weightまたはheightがnull、またはheightが0の場合はnull
     */
    public static BigDecimal calculate(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        // cmをmに変換
        BigDecimal heightInMeters = height.divide(CM_TO_METER, 2, RoundingMode.HALF_UP);
        
        // BMI = 体重(kg) / (身長(m))²
        BigDecimal bmi = weight.divide(
            heightInMeters.multiply(heightInMeters), 
            2, 
            RoundingMode.HALF_UP
        );
        
        return bmi;
    }
}

