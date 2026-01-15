package com.example.fitnessgym_mg.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * エラーレスポンスDTO
 * 
 * <p>REST APIのエラーレスポンスを統一された形式で返すためのDTOです。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>各エラーレスポンスには正確なタイムスタンプが必要なため、コンストラクタで生成します</li>
 *   <li>複数のエラーメッセージに対応するため、errorsフィールドをオプショナルで提供します</li>
 * </ul>
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private List<String> errors; // 複数エラー対応（オプショナル）
    private OffsetDateTime timestamp;
    
    /**
     * エラーレスポンスを生成（単一メッセージ）
     * 
     * <p>タイムスタンプは各レスポンスごとに正確な時刻を記録するため、
     * コンストラクタ内で生成します。</p>
     * 
     * @param code エラーコード
     * @param message エラーメッセージ
     */
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.errors = null;
        this.timestamp = OffsetDateTime.now();
    }
    
    /**
     * エラーレスポンスを生成（複数メッセージ）
     * 
     * <p>タイムスタンプは各レスポンスごとに正確な時刻を記録するため、
     * コンストラクタ内で生成します。</p>
     * 
     * @param code エラーコード
     * @param message エラーメッセージ（最初のメッセージ）
     * @param errors エラーメッセージのリスト
     */
    public ErrorResponse(String code, String message, List<String> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.timestamp = OffsetDateTime.now();
    }
}


