package com.example.fitnessgym_mg.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private List<String> errors; // 複数エラー対応（オプショナル）
    private OffsetDateTime timestamp;
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.errors = null;
        this.timestamp = OffsetDateTime.now();
    }
    
    public ErrorResponse(String code, String message, List<String> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.timestamp = OffsetDateTime.now();
    }
}


