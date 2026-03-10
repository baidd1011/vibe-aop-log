package com.vibelog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志配置属性类
 */
@Data
@ConfigurationProperties(prefix = "vibe.log")
public class LogProperties {

    /**
     * 是否启用日志记录
     */
    private boolean enabled = true;

    /**
     * 日志目录
     */
    private String logDir = "logs/vibe-log";

    /**
     * 日志文件名
     */
    private String fileName = "method.log";

    /**
     * 单个日志文件最大大小
     */
    private String maxFileSize = "10MB";

    /**
     * 保留历史文件天数
     */
    private int maxHistory = 30;

    /**
     * 排除的类名模式列表
     */
    private List<String> excludeClasses = new ArrayList<>();

    /**
     * 默认记录入参
     */
    private boolean params = true;

    /**
     * 默认记录返回值
     */
    private boolean result = true;

    /**
     * 默认记录执行耗时
     */
    private boolean time = true;

    /**
     * 默认记录异常
     */
    private boolean exception = true;
}