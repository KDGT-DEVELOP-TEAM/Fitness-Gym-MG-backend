package com.example.fitnessgym_mg.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ページ番号のバリデーションアノテーション
 * 
 * <p>ApplicationConstantsの値を参照して、ページ番号の範囲を検証します。</p>
 * <p>これにより、定数値とバリデーション値の不整合を防止します。</p>
 */
@Documented
@Constraint(validatedBy = PageValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPage {
    String message() default "Page must be 0 or greater and not exceed the maximum page number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
