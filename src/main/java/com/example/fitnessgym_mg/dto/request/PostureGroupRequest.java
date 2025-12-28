package com.example.fitnessgym_mg.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

@Data
public class PostureGroupRequest {
    
    @NotNull(message = "レッスンIDは必須です")
    private UUID lessonId;
    
    /**
     * 撮影日時（タイムゾーンを含む実時刻）
     * 
     * <p>クライアントから送信されない場合はサーバー側で現在時刻を設定します。</p>
     * <p>未来日時は指定できません。過去または現在の日時のみ有効です。</p>
     * 
     * <p>姿勢画像は「実世界の時刻」に近い概念のため、タイムゾーン情報を保持する{@link OffsetDateTime}を使用します。</p>
     * <p>これにより、異なるタイムゾーンからのアクセスや、時系列表示・比較・集計処理において正確な時刻情報を保持できます。</p>
     */
    @PastOrPresent(message = "撮影日時は現在以前である必要があります")
    private OffsetDateTime capturedAt;
}

