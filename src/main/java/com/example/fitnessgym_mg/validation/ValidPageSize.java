package com.example.fitnessgym_mg.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ページサイズのバリデーションアノテーション
 * 
 * <p>ApplicationConstantsの値を参照して、ページサイズの範囲を検証します。</p>
 * <p>これにより、定数値とバリデーション値の不整合を防止します。</p>
 */
@Documented
@Constraint(validatedBy = PageSizeValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPageSize {
    String message() default "Size must be at least 1 and not exceed the maximum page size";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
