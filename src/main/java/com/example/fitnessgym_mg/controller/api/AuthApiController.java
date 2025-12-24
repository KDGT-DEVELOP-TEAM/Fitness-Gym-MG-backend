package com.example.fitnessgym_mg.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.request.LoginRequest;
import com.example.fitnessgym_mg.dto.response.LoginResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.util.JwtTokenUtil;
import com.example.fitnessgym_mg.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 認証・認可REST APIコントローラー
 * Reactフロントエンドとの連携用
 * 
 * JWT（JSON Web Token）認証を使用
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * GET /api/auth/login
     * 認証状態の確認
     * 既に認証されている場合はユーザー情報を返す
     */
    @GetMapping("/login")
    public ResponseEntity<?> getLogin() {
        // 既に認証されている場合はユーザー情報を返す
        return securityUtil.getCurrentUser()
                .map(user -> ResponseEntity.ok(createLoginResponse(user, null)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * POST /api/auth/login
     * ログイン処理、JWTトークンを発行
     * 
     * フロントエンドは取得したトークンをlocalStorageに保存し、
     * 以降のリクエストでAuthorization: Bearer <token>ヘッダーを付与する
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        
        try {
            // 認証処理
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // セキュリティコンテキストに認証情報を設定
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // ユーザー情報を取得
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);
            
            if (user == null) {
                // ユーザーが存在しない場合も、認証失敗として扱う（情報漏洩防止）
                log.warn("ログイン失敗: ユーザーが見つかりません");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // JWTトークンを生成
            String token = jwtTokenUtil.generateToken(user);

            // レスポンス作成
            LoginResponse response = createLoginResponse(user, token);
            
            log.debug("ログイン成功: ユーザーID={}, 権限={}", user.getId(), user.getRole());
            
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("ログイン失敗: 認証エラー");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } catch (Exception e) {
            log.error("ログイン処理でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/auth/logout
     * ログアウト処理
     * 
     * JWTはステートレスなので、サーバー側では特に処理しない
     * クライアント側でトークンを削除することでログアウトが完了する
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // セキュリティコンテキストをクリア
        SecurityContextHolder.clearContext();
        
        log.debug("ログアウト成功");
        return ResponseEntity.ok().build();
    }

    /**
     * LoginResponseを作成
     */
    private LoginResponse createLoginResponse(User user, String token) {
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .token(token)
                .build();
    }
}
