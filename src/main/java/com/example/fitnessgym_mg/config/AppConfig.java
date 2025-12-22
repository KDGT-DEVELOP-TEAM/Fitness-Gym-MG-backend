package com.example.fitnessgym_mg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.example.fitnessgym_mg.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * アプリケーション設定クラス
 * ObjectMapperとJsonUtilsのBean定義を提供
 */
@Configuration
public class AppConfig {

	/**
	 * JSON変換用のObjectMapper Bean
	 * Spring Bootの推奨方法であるJackson2ObjectMapperBuilderを使用して作成
	 * JavaTimeModuleが自動的に登録されるため、OffsetDateTimeなどのシリアライズが可能
	 * 
	 * @param builder Jackson2ObjectMapperBuilder
	 * @return ObjectMapperインスタンス
	 */
	@Bean
	public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
		return builder.build();
	}

	/**
	 * JSON変換ユーティリティBean
	 * 
	 * @param objectMapper ObjectMapperインスタンス
	 * @return JsonUtilsインスタンス
	 */
	@Bean
	public JsonUtils jsonUtils(ObjectMapper objectMapper) {
		return new JsonUtils(objectMapper);
	}
}
