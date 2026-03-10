package com.vibelog;

import com.vibelog.aspect.LogAspect;
import com.vibelog.config.LogProperties;
import com.vibelog.writer.FileLogWriter;
import com.vibelog.writer.LogWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Vibe Log 自动装配类
 */
@AutoConfiguration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "vibe.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VibeLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogWriter logWriter(LogProperties properties) {
        return new FileLogWriter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogAspect logAspect(LogWriter logWriter, LogProperties properties) {
        return new LogAspect(logWriter, properties);
    }
}