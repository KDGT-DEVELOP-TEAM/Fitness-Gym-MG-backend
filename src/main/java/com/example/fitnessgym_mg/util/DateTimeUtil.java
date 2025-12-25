package com.example.fitnessgym_mg.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 日時変換ユーティリティクラス
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>APIはすべてUTCで返す（実行環境に依存しない）</li>
 *   <li>ユーザーのタイムゾーン変換はフロントエンドで実施</li>
 *   <li>DBに保存されているLocalDateTimeはUTCとして扱う</li>
 * </ul>
 * 
 * <p>理由:</p>
 * <ul>
 *   <li>サーバ移設・Docker化で時刻ズレが発生しない</li>
 *   <li>時系列データの整合性を保証</li>
 *   <li>フロントエンドでユーザーのタイムゾーンに変換可能</li>
 * </ul>
 */
public final class DateTimeUtil {
    
    private DateTimeUtil() {
        // インスタンス化を防ぐ
    }
    
    /**
     * LocalDateTimeをUTC固定のOffsetDateTimeに変換
     * 
     * <p>APIレスポンス用の変換メソッド。
     * DBに保存されているLocalDateTimeをUTCとして扱い、OffsetDateTimeに変換します。</p>
     * 
     * @param localDateTime 変換元のLocalDateTime（nullの場合はnullを返す）
     * @return UTC固定のOffsetDateTime、localDateTimeがnullの場合はnull
     */
    public static OffsetDateTime toUtcOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}

