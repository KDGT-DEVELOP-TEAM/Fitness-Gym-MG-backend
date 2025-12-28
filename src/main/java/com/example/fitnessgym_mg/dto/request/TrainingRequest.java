package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.fitnessgym_mg.config.ApplicationConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRequest {
    
    /**
     * 順序番号
     * 
     * <p>トレーニングの表示順序を指定します。1以上の整数値である必要があります。</p>
     * <p>このフィールドは「表示用」であり、クライアントから送信された値がそのまま使用されます。</p>
     * <p>重複チェックや連番性の正規化はService層（{@link com.example.fitnessgym_mg.service.TrainingService}）で実施されます。</p>
     * <p>DTO層では値の存在と下限のみを保証し、一意性・連番性の保証は行いません。</p>
     */
    @NotNull(message = "順序番号は必須です")
    @Min(value = 1, message = "順序番号は1以上である必要があります")
    private Integer orderNo;
    
    @NotBlank(message = "トレーニング名は必須です")
    @Size(max = 100, message = "トレーニング名は100文字以内で入力してください")
    private String name;
    
    /**
     * 実施回数
     * 
     * <p>トレーニングの実施回数を指定します。1回以上である必要があります。</p>
     * <p>このフィールドは「実施したトレーニング」を前提としており、0回は許可されません。</p>
     * <p>メニューのみを登録する場合は、別の設計を検討してください。</p>
     */
    @NotNull(message = "回数は必須です")
    @Min(value = 1, message = "回数は1以上である必要があります")
    @Max(value = ApplicationConstants.MAX_TRAINING_REPS, message = "回数は" + ApplicationConstants.MAX_TRAINING_REPS + "以下である必要があります")
    private Integer reps;
}

