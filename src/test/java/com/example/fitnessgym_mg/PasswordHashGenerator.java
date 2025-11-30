package com.example.fitnessgym_mg;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * パスワードのBCryptハッシュを生成するためのユーティリティクラス
 * デモアカウント追加時に使用
 * 
 * 実行方法:
 * 1. Eclipseでこのファイルを開く
 * 2. ファイルを右クリック → 「実行」→ 「Java アプリケーション」
 * または
 * 3. エディタ上部の実行ボタン（緑の再生ボタン）をクリック
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // デモアカウントのパスワードをハッシュ化
        String[] passwords = {"password", "admin123", "trainer123", "demo123"};
        
        System.out.println("========================================");
        System.out.println("=== BCrypt Password Hashes ===");
        System.out.println("========================================");
        System.out.println();
        
        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("Hash:     " + hash);
            System.out.println();
        }
        
        System.out.println("========================================");
        System.out.println("上記のハッシュ値をSupabaseのusersテーブルのpassカラムにコピーしてください");
        System.out.println("========================================");
    }
}

