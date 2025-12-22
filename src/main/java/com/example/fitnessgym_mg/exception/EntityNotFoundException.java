package com.example.fitnessgym_mg.exception;

/**
 * エンティティが見つからない場合にスローされる例外
 */
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
