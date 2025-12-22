package com.example.fitnessgym_mg.util;

import com.example.fitnessgym_mg.exception.JsonConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * JSON変換ユーティリティクラス
 * Thymeleafテンプレート内で使用する
 */
@RequiredArgsConstructor
public class JsonUtils {
    
    private final ObjectMapper objectMapper;

    /**
     * オブジェクトをJSON文字列に変換
     * 
     * @param obj 変換対象のオブジェクト
     * @return JSON文字列
     * @throws JsonConversionException JSON変換に失敗した場合
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSON変換に失敗しました", e);
        }
    }
}
