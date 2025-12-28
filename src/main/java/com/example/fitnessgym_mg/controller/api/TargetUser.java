package com.example.fitnessgym_mg.controller.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * メソッドパラメータにUserエンティティを注入するためのアノテーション
 * HandlerMethodArgumentResolverでuserIdからUserを取得して注入する
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetUser {
}

