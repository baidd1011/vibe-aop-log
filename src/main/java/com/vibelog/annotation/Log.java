package com.vibelog.annotation;

import java.lang.annotation.*;

/**
 * 日志记录注解
 * 标记在方法上，用于记录方法执行日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 描述
     */
    String value() default "";

    /**
     * 是否记录入参
     */
    boolean params() default true;

    /**
     * 是否记录返回值
     */
    boolean result() default true;

    /**
     * 是否记录执行耗时
     */
    boolean time() default true;

    /**
     * 是否记录异常信息
     */
    boolean exception() default true;
}