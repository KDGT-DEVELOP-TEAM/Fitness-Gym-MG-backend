package com.example.fitnessgym_mg.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginAttemptFilter extends OncePerRequestFilter {
    
    private final LoadingCache<String, Integer> attemptsCache;
    
    public LoginAttemptFilter() {
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        
        if (request.getRequestURI().equals("/api/auth/login") 
                && request.getMethod().equals("POST")) {
            
            String clientIP = getClientIP(request);
            
            try {
                int attempts = attemptsCache.get(clientIP);
                
                if (attempts >= 5) {
                    response.setStatus(429);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"error\":\"ログイン試行回数が上限に達しました。5分後に再試行してください\"}"
                    );
                    return;
                }
                
                attemptsCache.put(clientIP, attempts + 1);
                
            } catch (Exception e) {
                log.error("Rate limit check failed", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

