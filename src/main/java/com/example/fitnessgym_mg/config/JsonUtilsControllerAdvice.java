package com.example.fitnessgym_mg.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.fitnessgym_mg.util.JsonUtils;

import lombok.RequiredArgsConstructor;

/**
 * すべてのコントローラーでjsonUtilsをModelに追加
 * Thymeleafテンプレート内でjsonUtilsを使用可能にする
 */
@ControllerAdvice
@RequiredArgsConstructor
public class JsonUtilsControllerAdvice {
    
    private final JsonUtils jsonUtils;

    /**
     * ModelにjsonUtilsを追加
     * 
     * @return JsonUtilsインスタンス
     */
    @ModelAttribute("jsonUtils")
    public JsonUtils jsonUtils() {
        return jsonUtils;
    }
}
