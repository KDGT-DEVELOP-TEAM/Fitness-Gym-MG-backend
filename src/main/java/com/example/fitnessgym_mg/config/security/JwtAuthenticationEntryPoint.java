package com.example.fitnessgym_mg.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT認証エントリーポイント
 * 
 * <p>認証失敗時のエラーレスポンス生成を担当します。</p>
 * <p>JwtAuthenticationFilterからエラーハンドリングの責務を分離し、
 * テストの容易性とエラーハンドリングの一貫性を向上させます。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        log.debug("認証エラー: {}", authException.getMessage());
        
        // デフォルトのエラーレスポンス
        String errorCode = "JWT_AUTHENTICATION_FAILED";
        String message = "認証に失敗しました";
        
        // エラーメッセージからエラーコードとメッセージを抽出（JwtAuthenticationFilterから設定された場合）
        String exceptionMessage = authException.getMessage();
        if (exceptionMessage != null && exceptionMessage.contains(":")) {
            String[] parts = exceptionMessage.split(":", 2);
            if (parts.length == 2) {
                errorCode = parts[0].trim();
                message = parts[1].trim();
            }
        }
        
        sendUnauthorized(response, errorCode, message);
    }
    
    /**
     * 401 エラーレスポンスを返す
     * 
     * <p>JwtAuthenticationFilterから呼び出されるヘルパーメソッドです。</p>
     * 
     * @param response HTTPレスポンス
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     */
    public void sendUnauthorized(
            HttpServletResponse response,
            String errorCode,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
