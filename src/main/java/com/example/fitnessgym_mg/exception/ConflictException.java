package com.example.fitnessgym_mg.exception;

/**
 * リソースの競合を表す例外
 * HTTPステータスコード409 Conflictに対応
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

