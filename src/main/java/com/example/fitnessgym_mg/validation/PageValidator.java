package com.example.fitnessgym_mg.validation;

import com.example.fitnessgym_mg.config.ApplicationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * ページ番号のバリデーター実装
 * 
 * <p>ApplicationConstants.MAX_PAGE_NUMBERを参照して、ページ番号の範囲を検証します。</p>
 */
public class PageValidator implements ConstraintValidator<ValidPage, Integer> {
    
    @Override
    public void initialize(ValidPage constraintAnnotation) {
        // 初期化処理は不要
    }
    
    @Override
    public boolean isValid(Integer page, ConstraintValidatorContext context) {
        if (page == null) {
            return true; // nullチェックは別のアノテーションで実施
        }
        
        // ApplicationConstantsの値を参照して検証
        if (page < 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Page must be 0 or greater")
                .addConstraintViolation();
            return false;
        }
        
        if (page > ApplicationConstants.MAX_PAGE_NUMBER) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Page must not exceed " + ApplicationConstants.MAX_PAGE_NUMBER)
                .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
