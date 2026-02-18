package com.example.fitnessgym_mg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC設定クラス
 * ArgumentResolverの登録など、Web MVC関連の設定を担当
 * 
 * <p>責務の分離:</p>
 * <ul>
 *   <li>ForwardedHeaderFilterの設定はSecurityConfigに移動しました（セキュリティ関連のため）</li>
 *   <li>このクラスはArgumentResolverの登録のみを担当します</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TargetUserArgumentResolver targetUserArgumentResolver;

    public WebConfig(TargetUserArgumentResolver targetUserArgumentResolver) {
        this.targetUserArgumentResolver = targetUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(targetUserArgumentResolver);
    }
}
