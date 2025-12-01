package com.example.fitnessgym_mg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * アプリケーション設定クラス
 * Thymeleafの設定とJSON変換ユーティリティを提供
 */
@Configuration
public class AppConfig {

	/**
	 * JSON変換用のObjectMapper Bean
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	/**
	 * JSON変換ユーティリティBean
	 */
	@Bean
	public JsonUtils jsonUtils(ObjectMapper objectMapper) {
		return new JsonUtils(objectMapper);
	}

	/**
	 * JSON変換ユーティリティクラス
	 * Thymeleafテンプレート内で使用する
	 */
	public static class JsonUtils {
		private final ObjectMapper objectMapper;

		public JsonUtils(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		public String toJson(Object obj) {
			try {
				return objectMapper.writeValueAsString(obj);
			} catch (Exception e) {
				return "{}";
			}
		}
	}

	/**
	 * すべてのコントローラーでjsonUtilsをModelに追加
	 */
	@ControllerAdvice
	public static class JsonUtilsControllerAdvice {
		private final JsonUtils jsonUtils;

		public JsonUtilsControllerAdvice(JsonUtils jsonUtils) {
			this.jsonUtils = jsonUtils;
		}

		@ModelAttribute("jsonUtils")
		public JsonUtils jsonUtils() {
			return jsonUtils;
		}
	}
}

