package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 顧客レスポンスDTO
 * 
 * 画面に表示する顧客情報を含む
 * Entity（Customer）から必要な情報だけを抽出して、このDTOに変換して返す
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    
    private UUID id;
    private String name;
    private String kana;
    private String email;
    private String phone;
    private LocalDate birthdate;
    private BigDecimal height;
    private String gender;
    private String address;
    
    // 最新レッスン情報（オプション）
    private BigDecimal latestWeight;
    private LocalDate latestLessonDate;
}

