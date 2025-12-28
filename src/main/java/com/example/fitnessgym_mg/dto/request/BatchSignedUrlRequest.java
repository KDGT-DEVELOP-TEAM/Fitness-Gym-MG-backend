package com.example.fitnessgym_mg.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

import com.example.fitnessgym_mg.config.ApplicationConstants;

@Data
public class BatchSignedUrlRequest {
    
    /**
     * 画像IDリスト
     * 
     * <p>1件以上、最大50件まで指定できます。</p>
     * <p>署名URL生成はCPU/I/Oコストが高い処理のため、件数制限を設けています。</p>
     */
    @NotEmpty(message = "imageIdsは必須で、1件以上指定してください")
    @Size(max = ApplicationConstants.MAX_BATCH_SIGNED_URL_COUNT, message = "imageIdsは最大" + ApplicationConstants.MAX_BATCH_SIGNED_URL_COUNT + "件まで指定できます")
    private List<UUID> imageIds;
    
    /**
     * 署名付きURLの有効期限（秒）
     * 
     * <p>クライアントが指定しない場合は、Service層でデフォルト値（{@link ApplicationConstants#DEFAULT_SIGNED_URL_EXPIRES_IN}）が適用されます。</p>
     * 
     * <p>有効範囲: {@link ApplicationConstants#MIN_SIGNED_URL_EXPIRES_IN}秒（60秒）以上、
     * {@link ApplicationConstants#MAX_SIGNED_URL_EXPIRES_IN}秒（604800秒、7日）以下</p>
     */
    @NotNull(message = "expiresInは必須です")
    @Min(value = ApplicationConstants.MIN_SIGNED_URL_EXPIRES_IN, message = "expiresInは" + ApplicationConstants.MIN_SIGNED_URL_EXPIRES_IN + "秒以上である必要があります")
    @Max(value = ApplicationConstants.MAX_SIGNED_URL_EXPIRES_IN, message = "expiresInは" + ApplicationConstants.MAX_SIGNED_URL_EXPIRES_IN + "秒（7日）以下である必要があります")
    private Integer expiresIn;
}
