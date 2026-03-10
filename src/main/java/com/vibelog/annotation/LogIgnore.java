package com.vibelog.annotation;

import java.lang.annotation.*;

/**
 * 日志忽略注解
 * 标记在参数或字段上，用于忽略该参数的记录
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogIgnore {
}