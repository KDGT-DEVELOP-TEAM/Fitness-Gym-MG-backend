package com.example.fitnessgym_mg.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.request.LoginRequest;
import com.example.fitnessgym_mg.dto.response.LoginResponse;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.exception.AuthenticationStateException;
import com.example.fitnessgym_mg.service.AccountService;
import com.example.fitnessgym_mg.util.JwtTokenUtil;
import com.example.fitnessgym_mg.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final AccountService accountService;
    private final SecurityUtil securityUtil;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * GET /api/auth/me
     * 認証状態の確認
     * 既に認証されている場合はユーザー情報を返す
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        // 既に認証されている場合はユーザー情報を返す
        // storesも一緒に取得するため、emailで再取得
        return securityUtil.getCurrentUser()
                .map(user -> {
                    // storesを読み込むために再取得
                    User userWithStores = accountService.findUserByEmailWithStores(user.getEmail())
                            .orElse(user); // 取得できない場合は元のuserを使用
                    return ResponseEntity.ok(createLoginResponse(userWithStores, null));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * GET /api/auth/login
     * 認証状態の確認
     * 既に認証されている場合はユーザー情報を返す
     * 
     * @deprecated GET /api/auth/me を使用してください
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/login")
    public ResponseEntity<?> getLogin() {
        // 既に認証されている場合はユーザー情報を返す
        // storesも一緒に取得するため、emailで再取得
        return securityUtil.getCurrentUser()
                .map(user -> {
                    // storesを読み込むために再取得
                    User userWithStores = accountService.findUserByEmailWithStores(user.getEmail())
                            .orElse(user); // 取得できない場合は元のuserを使用
                    return ResponseEntity.ok(createLoginResponse(userWithStores, null));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * POST /api/auth/login
     * ログイン処理、JWTトークンを発行
     * 
     * <p>トークンの保存方法:</p>
     * <ul>
     *   <li>フロントエンドは取得したトークンをlocalStorageに保存し、
     *   以降のリクエストでAuthorization: Bearer &lt;token&gt; ヘッダーを付与する</li>
     *   <li>UXを優先し、タブを閉じてもログイン状態が保持される</li>
     * </ul>
     * 
     * <p>セキュリティ注意事項:</p>
     * <ul>
     *   <li>localStorageはJavaScriptからアクセス可能なため、XSS攻撃でトークンが漏洩するリスクがある</li>
     *   <li>フロントエンド側でXSS対策（入力値のサニタイズ、CSP設定など）を徹底すること</li>
     *   <li>本番環境では、可能な限りHttpOnly Cookieの使用を検討すること</li>
     * </ul>
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
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

            // ユーザー情報を取得（認証成功後なので必ず存在するはず、active条件を適用）
            // storesも一緒に取得するため、findUserByEmailWithStoresを使用
            User user = accountService.findUserByEmailWithStores(request.getEmail())
                .orElseThrow(() -> {
                    log.error("認証成功後にユーザーが見つからない異常事態を検出");
                    return new AuthenticationStateException("ユーザー情報の取得に失敗しました");
                });

            // JWTトークンを生成
            String token = jwtTokenUtil.generateToken(user);

            // レスポンス作成（トークンを含める）
            LoginResponse responseBody = createLoginResponse(user, token);
            
            // ログ出力（権限情報は機密性が高いため除外）
            log.info("ログイン成功: ユーザーID={}", user.getId());
            
            return ResponseEntity.ok(responseBody);
        } catch (BadCredentialsException e) {
            // 認証失敗: メールアドレスまたはパスワードが正しくない
            log.warn("ログイン失敗試行を検出");
            // メールアドレスの存在有無を推測させないため、統一されたエラーメッセージ
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "認証に失敗しました"));
        } catch (Exception e) {
            // その他の予期しないエラー
            log.error("認証処理中にエラーが発生: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "認証処理中にエラーが発生しました"));
        }
    }

    /**
     * POST /api/auth/logout
     * ログアウト処理
     * 
     * JWTはステートレスなので、サーバー側では特に処理しない
     * クライアント側でlocalStorageからトークンを削除することでログアウトが完了する
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // セキュリティコンテキストをクリア
        SecurityContextHolder.clearContext();
        
        log.debug("ログアウト成功");
        return ResponseEntity.ok().build();
    }

    /**
     * LoginResponseを作成
     * 
     * <p>注意: ログイン機能では、フロントエンドでユーザー識別のためにメールアドレスが必要な場合があるため、
     * レスポンスにメールアドレスを含めています。セキュリティリスクを最小限にするため、
     * フロントエンド側で適切なセキュリティ対策（XSS対策、CSP設定など）を実施してください。</p>
     */
    private LoginResponse createLoginResponse(User user, String token) {
        // stores関係が設定されているかチェック
        Set<UUID> storeIds = Set.of();
        if (user.getStores() != null && !user.getStores().isEmpty()) {
            storeIds = user.getStores().stream()
                    .map(Store::getId)
                    .collect(Collectors.toSet());
        }
        
        return LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail()) // ログイン機能ではユーザー識別のために必要
                .name(user.getName())
                .role(user.getRole())
                .storeIds(storeIds)
                .token(token)
                .build();
    }
}
