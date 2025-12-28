package com.example.fitnessgym_mg.util;

import org.springframework.data.domain.Pageable;

import com.example.fitnessgym_mg.config.ApplicationConstants;
import com.example.fitnessgym_mg.exception.InvalidRequestException;

/**
 * Pageableオブジェクトの検証を行うユーティリティクラス
 * 
 * <p>DoS対策として、巨大なOFFSETクエリによるDB負荷を防ぐための検証を提供します。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>すべてのPageableを受け取るServiceメソッドで、この検証を適用することを推奨</li>
 *   <li>Controller層での早期リターンとは別に、Service層でも最終防衛線として検証</li>
 * </ul>
 */
public final class PageableValidator {

    private PageableValidator() {
        // インスタンス化を防ぐ
    }

    /**
     * Pageableオブジェクトを検証する
     * 
     * <p><b>DoS対策:</b> 以下の検証を実施します:</p>
     * <ul>
     *   <li>pageNumberが負数でないことを確認</li>
     *   <li>pageSizeが0より大きいことを確認（下限チェック）</li>
     *   <li>pageSizeが上限（{@link ApplicationConstants#MAX_PAGE_SIZE}）を超えていないことを確認</li>
     *   <li>offset（page × size）が負数でないことを確認（防御的プログラミング）</li>
     *   <li>offset（page × size）が上限（{@link ApplicationConstants#MAX_OFFSET}）を超えていないことを確認</li>
     * </ul>
     * 
     * <p><b>セキュリティ考慮事項:</b></p>
     * <ul>
     *   <li>int型のオーバーフローを防ぐため、offsetはlong型で計算し、Spring Dataの標準メソッド{@link Pageable#getOffset()}を使用</li>
     *   <li>巨大なOFFSETクエリは、PostgreSQLなどのデータベースでパフォーマンス問題を引き起こす可能性があります</li>
     *   <li>巨大なpageSizeは、メモリ圧迫・OOMのリスクがあります（特にJOINが絡む場合）</li>
     * </ul>
     * 
     * <p><b>防御的プログラミング:</b></p>
     * <ul>
     *   <li>pageSize <= 0のチェックにより、無効な入力（pageSize == 0、pageSize < 0）を早期に検出</li>
     *   <li>offset < 0のチェックにより、カスタムPageable実装での予期しない動作を防止</li>
     *   <li>これらのチェックにより、無駄なクエリやDB/Dialect依存の挙動を防ぎます</li>
     * </ul>
     * 
     * <p>この検証により、意図的または偶発的な大量データ取得を防ぎます。</p>
     * 
     * @param pageable 検証対象のPageableオブジェクト
     * @throws InvalidRequestException 検証に失敗した場合（pageNumber < 0、pageSize <= 0、pageSize > MAX_PAGE_SIZE、offset < 0、offset > MAX_OFFSET）
     * @throws IllegalArgumentException pageableがnullの場合
     */
    public static void validateOffset(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must not be null");
        }

        // pageNumberの負数チェック
        if (pageable.getPageNumber() < 0) {
            throw new InvalidRequestException("Page number must be >= 0");
        }

        // pageSizeの下限チェック（MAXチェックより前に配置）
        if (pageable.getPageSize() <= 0) {
            throw new InvalidRequestException("Page size must be > 0");
        }

        // pageSizeの上限チェック
        if (pageable.getPageSize() > ApplicationConstants.MAX_PAGE_SIZE) {
            throw new InvalidRequestException(
                "Page size too large. Maximum page size is " + ApplicationConstants.MAX_PAGE_SIZE
            );
        }

        // offsetの上限チェック（long型で計算し、オーバーフローを防ぐ）
        // Spring Dataの標準メソッドgetOffset()を使用することで、フレームワークの実装に追従
        long offset = pageable.getOffset();
        
        // offsetの負数チェック（防御的プログラミング）
        // カスタムPageable実装ではgetOffset()が負数を返す可能性があるため
        if (offset < 0) {
            throw new InvalidRequestException("Offset must be >= 0");
        }
        
        if (offset > ApplicationConstants.MAX_OFFSET) {
            throw new InvalidRequestException(
                "Offset too large. Maximum offset is " + ApplicationConstants.MAX_OFFSET
            );
        }
    }
}

