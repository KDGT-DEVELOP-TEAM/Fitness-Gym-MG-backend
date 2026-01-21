package com.example.fitnessgym_mg.config;

import java.time.ZoneId;

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
     * 最大ページサイズ
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    /**
     * 最小ページサイズ
     */
    public static final int MIN_PAGE_SIZE = 1;

    /**
     * 最大ページ番号
     * 巨大なOFFSETクエリを防ぐため、1000ページまでに制限
     */
    public static final int MAX_PAGE_NUMBER = 1000;
    
    /**
     * 最大オフセット値
     * page × size がこの値を超える場合、InvalidRequestExceptionをスロー
     * 巨大なOFFSETクエリによるDB負荷を防ぐため、10000件までに制限
     * 
     * <p>long型を使用することで、int型のオーバーフローを防ぎます。</p>
     */
    public static final long MAX_OFFSET = 10000L;

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
    
    /**
     * バッチ署名付きURL生成の最大件数
     * 署名URL生成はCPU/I/Oコストが高い処理のため、件数制限を設ける
     */
    public static final int MAX_BATCH_SIGNED_URL_COUNT = 50;

    // ===== レッスン関連 =====
    
    /**
     * 1レッスンあたりの最大トレーニング件数
     * パフォーマンス・DoS対策のため、件数制限を設ける
     */
    public static final int MAX_LESSON_TRAININGS_COUNT = 50;
    
    /**
     * トレーニングの最大実施回数
     * 現実的な上限値として10000回を設定
     */
    public static final int MAX_TRAINING_REPS = 10000;

    // ===== その他 =====
    
    /**
     * デフォルトタイムゾーン
     * 
     * <p>レッスンの日時検証などで使用するタイムゾーンです。</p>
     * <p>フロントエンドから送信される日時はローカルタイムゾーン（日本時間）であり、
     * それをサーバー側でも日本時間として比較します。</p>
     */
    public static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Asia/Tokyo");
}
