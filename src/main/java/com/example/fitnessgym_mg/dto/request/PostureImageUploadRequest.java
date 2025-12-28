package com.example.fitnessgym_mg.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;

@Data
public class PostureImageUploadRequest {
    @NotNull(message = "postureGroupIdは必須です")
    private UUID postureGroupId;
    
    /**
     * 撮影位置
     * 
     * <p>姿勢画像の撮影位置を指定します。</p>
     * <p>Springは自動でバインドしてくれ、不正値は400で即落ちます。</p>
     * <p>ドメインと完全に同期するため、型安全性が保証されます。</p>
     */
    @NotNull(message = "positionは必須です")
    private PostureImagePosition position;
    
    /**
     * 公開への同意フラグ
     * 
     * <p>明示的に`true`が指定された場合のみ公開可能です。</p>
     * <p>デフォルト値は`false`であり、同意しない限り公開されません。</p>
     * <p>これはプライバシー配慮として正しい初期値であり、セキュリティ事故を防ぐ方向です。</p>
     */
    private boolean consentPublication = false;
    
    /**
     * 撮影日時（タイムゾーンを含む実時刻）
     * 
     * <p>クライアントから送信されない場合はサーバー側で現在時刻を設定します。</p>
     * <p>未来日時は指定できません。過去または現在の日時のみ有効です。</p>
     * 
     * <p>「現在時刻補完」はService層で統一して実施します。</p>
     */
    @PastOrPresent(message = "撮影日時は現在以前である必要があります")
    private OffsetDateTime takenAt;
}
