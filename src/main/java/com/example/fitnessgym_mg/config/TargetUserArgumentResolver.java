package com.example.fitnessgym_mg.config;

import java.util.Map;
import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import com.example.fitnessgym_mg.controller.api.TargetUser;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * @TargetUserアノテーションが付いたパラメータにUserエンティティを注入するResolver
 * パス変数のuserIdからUserを取得して注入する
 */
@Component
@RequiredArgsConstructor
public class TargetUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AccountService accountService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(TargetUser.class) 
            && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {
        
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        
        // パス変数マップからuser_idを取得
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request
            .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        
        String userIdStr = null;
        String storeIdStr = null;
        
        if (pathVariables != null) {
            userIdStr = pathVariables.get("user_id");
            storeIdStr = pathVariables.get("store_id");
        }
        
        if (userIdStr == null) {
            throw new IllegalArgumentException("user_idパス変数が見つかりません");
        }
        
        UUID userId = UUID.fromString(userIdStr);
        
        // storeIdを取得（Manager用エンドポイントの場合）
        UUID storeId = null;
        if (storeIdStr != null) {
            storeId = UUID.fromString(storeIdStr);
        }
        
        // AccountServiceからUserを取得
        return accountService.findUserEntityById(userId, storeId);
    }
}

