package com.example.fitnessgym_mg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC設定クラス
 * ビューコントローラーの設定を担当
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
    
    /**
     * ForwardedHeaderFilterをBeanとして登録
     * X-Forwarded-Forヘッダーを適切に処理し、信頼できるプロキシからのIPのみを取得
     */
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        FilterRegistrationBean<ForwardedHeaderFilter> registration = 
            new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
