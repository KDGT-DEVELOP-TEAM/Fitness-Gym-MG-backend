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
import com.example.fitnessgym_mg.util.SecurityUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 認証・認可REST APIコントローラー
 * Reactフロントエンドとの連携用
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

    /**
     * GET /api/auth/login
     * ログイン画面表示（React側で実装される想定）
     * このエンドポイントは主にReact側で使用されるため、認証状態を確認する程度
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
     * 認証情報送信、JWTトークン取得（またはセッショントークン）
     * 現在はセッションベース認証を使用
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
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
            // セキュリティ: ユーザーが見つからない場合も認証失敗として扱う（情報漏洩防止）
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);
            
            if (user == null) {
                // ユーザーが存在しない場合も、認証失敗として扱う（情報漏洩防止）
                log.warn("ログイン失敗: 認証エラー");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(null);
            }

            // セッションIDを取得（将来JWTに移行する場合はここでトークンを生成）
            // 認証成功後はセッションが作成されているはずなので、getSession(true)で取得
            HttpSession session = httpRequest.getSession(true);
            String token = session.getId();

            // レスポンス作成
            LoginResponse response = createLoginResponse(user, token);
            
            // セキュリティ: ログにはメールアドレスではなくユーザーIDを出力
            log.debug("ログイン成功: ユーザーID={}, 権限={}", user.getId(), user.getRole());
            
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            // セキュリティ: エラーメッセージを汎用的に（情報漏洩防止）
            log.warn("ログイン失敗: 認証エラー");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        } catch (Exception e) {
            log.error("ログイン処理でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/auth/logout
     * サーバー側セッション破棄/クライアント側トークン削除
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        try {
            // セッションを無効化
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            // セキュリティコンテキストをクリア
            SecurityContextHolder.clearContext();

            log.debug("ログアウト成功");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("ログアウト処理でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

