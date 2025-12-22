package com.example.fitnessgym_mg.controller.util;

import java.util.UUID;

/**
 * コントローラー層のパス判定ユーティリティ
 * リクエストパスからユーザータイプやパス情報を判定するロジックを共通化
 */
public class ControllerPathUtils {
    
    /**
     * リクエストパスからユーザータイプを判定
     * 
     * @param requestPath リクエストパス
     * @return ユーザータイプ情報（isAdmin, isManager, isTrainer）
     */
    public static UserTypeInfo determineUserTypeFromPath(String requestPath) {
        boolean isTrainer = requestPath.startsWith("/customer/") || 
                           requestPath.startsWith("/trainer/") || 
                           requestPath.startsWith("/lesson/");
        boolean isManager = requestPath.startsWith("/manager/");
        boolean isAdmin = !isTrainer && !isManager;
        return new UserTypeInfo(isAdmin, isManager, isTrainer);
    }
    
    /**
     * リクエストパスからベースパスと詳細ベースパスを生成
     * 
     * @param requestPath リクエストパス
     * @param storeId 店舗ID（MANAGERの場合に必要）
     * @param customerId 顧客ID
     * @return ベースパス情報（basePath, detailBasePath）
     */
    public static BasePathInfo determineBasePaths(String requestPath, UUID storeId, UUID customerId) {
        String basePath;
        String detailBasePath;
        if (requestPath.startsWith("/manager/")) {
            basePath = "/manager/" + storeId + "/history/" + customerId;
            detailBasePath = "/manager/" + storeId + "/lessons";
        } else if (requestPath.startsWith("/customer/")) {
            basePath = "/customer/" + customerId + "/lessons";
            detailBasePath = "/lesson";
        } else {
            basePath = "/admin/history/" + customerId;
            detailBasePath = "/admin/lessons";
        }
        return new BasePathInfo(basePath, detailBasePath);
    }
    
    /**
     * ユーザータイプ情報を保持するレコード
     */
    public record UserTypeInfo(boolean isAdmin, boolean isManager, boolean isTrainer) {}
    
    /**
     * ベースパス情報を保持するレコード
     */
    public record BasePathInfo(String basePath, String detailBasePath) {}
}
