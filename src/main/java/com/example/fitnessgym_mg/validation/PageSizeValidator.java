package com.example.fitnessgym_mg.validation;

import com.example.fitnessgym_mg.config.ApplicationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * ページサイズのバリデーター実装
 * 
 * <p>ApplicationConstantsの値を参照して、ページサイズの範囲を検証します。</p>
 */
public class PageSizeValidator implements ConstraintValidator<ValidPageSize, Integer> {
    
    @Override
    public void initialize(ValidPageSize constraintAnnotation) {
        // 初期化処理は不要
    }
    
    @Override
    public boolean isValid(Integer size, ConstraintValidatorContext context) {
        if (size == null) {
            return true; // nullチェックは別のアノテーションで実施
        }
        
        // ApplicationConstantsの値を参照して検証
        if (size < ApplicationConstants.MIN_PAGE_SIZE) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Size must be at least " + ApplicationConstants.MIN_PAGE_SIZE)
                .addConstraintViolation();
            return false;
        }
        
        if (size > ApplicationConstants.MAX_PAGE_SIZE) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Size must not exceed " + ApplicationConstants.MAX_PAGE_SIZE)
                .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
