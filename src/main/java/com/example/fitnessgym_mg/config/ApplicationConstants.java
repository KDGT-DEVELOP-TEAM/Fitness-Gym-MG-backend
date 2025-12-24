package com.example.fitnessgym_mg.config;

/**
 * アプリケーション全体で使用する定数クラス
 * マジックナンバーを定数として集約
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
        // インスタンス化を防ぐ
    }

    // ===== ページネーション関連 =====
    
    /**
     * デフォルトのページ番号
     */
    public static final int DEFAULT_PAGE_NUMBER = 0;
    
    /**
     * デフォルトのページサイズ
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    /**
     * 最大ページサイズ
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * 最小ページサイズ
     */
    public static final int MIN_PAGE_SIZE = 1;

    // ===== ファイルアップロード関連 =====
    
    /**
     * 最大ファイルサイズ（MB）
     */
    public static final int MAX_FILE_SIZE_MB = 10;
    
    /**
     * 最大ファイルサイズ（バイト）
     * MAX_FILE_SIZE_MB から自動計算される
     */
    public static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024L * 1024L;

    // ===== 署名付きURL関連 =====
    
    /**
     * デフォルトの署名付きURL有効期限（秒）
     * 1時間 = 3600秒
     */
    public static final int DEFAULT_SIGNED_URL_EXPIRES_IN = 3600;
    
    /**
     * 最小の署名付きURL有効期限（秒）
     * 60秒
     */
    public static final int MIN_SIGNED_URL_EXPIRES_IN = 60;
    
    /**
     * 最大の署名付きURL有効期限（秒）
     * 7日 = 604800秒
     */
    public static final int MAX_SIGNED_URL_EXPIRES_IN = 604800;

    // ===== 文字列長制限 =====
    
    /**
     * キーワード検索の最大文字数
     */
    public static final int MAX_KEYWORD_LENGTH = 100;
    
    /**
     * 名前・かなの最大文字数
     */
    public static final int MAX_NAME_LENGTH = 100;
    
    /**
     * メモ・コンディション・食事内容の最大文字数
     */
    public static final int MAX_MEMO_LENGTH = 500;
    
    /**
     * 詳細メモの最大文字数
     */
    public static final int MAX_DETAIL_MEMO_LENGTH = 1000;
    
    /**
     * 体重の最大値（kg）
     */
    public static final int MAX_WEIGHT = 500;
    
    /**
     * 身長の最小値（cm）
     */
    public static final int MIN_HEIGHT = 50;
    
    /**
     * 身長の最大値（cm）
     */
    public static final int MAX_HEIGHT = 300;

    // ===== その他 =====
    
    /**
     * BMI計算時の身長変換係数（cmからmへの変換）
     */
    public static final int HEIGHT_CONVERSION_FACTOR = 100;
}
