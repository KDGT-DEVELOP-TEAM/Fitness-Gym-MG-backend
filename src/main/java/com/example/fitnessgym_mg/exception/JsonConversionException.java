package com.example.fitnessgym_mg.exception;

/**
 * JSON変換処理でエラーが発生した場合にスローされる例外
 */
public class JsonConversionException extends RuntimeException {
    
    public JsonConversionException(String message) {
        super(message);
    }
    
    public JsonConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
