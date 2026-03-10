package com.vibelog.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibelog.annotation.Log;
import com.vibelog.annotation.LogIgnore;
import com.vibelog.config.LogProperties;
import com.vibelog.context.TraceContext;
import com.vibelog.writer.LogWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 日志记录切面
 */
@Slf4j
@Aspect
public class LogAspect {

    private final LogWriter logWriter;
    private final LogProperties properties;
    private final ObjectMapper objectMapper;
    private final SimpleDateFormat dateFormat;

    private final List<Pattern> excludePatterns;

    public LogAspect(LogWriter logWriter, LogProperties properties) {
        this.logWriter = logWriter;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        // 编译排除类名模式
        this.excludePatterns = new ArrayList<>();
        if (properties.getExcludeClasses() != null) {
            for (String pattern : properties.getExcludeClasses()) {
                excludePatterns.add(Pattern.compile(pattern.replace(".", "\\.").replace("*", ".*")));
            }
        }
    }

    @Around("@annotation(com.vibelog.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否需要排除
        String className = joinPoint.getTarget().getClass().getName();
        if (isExcluded(className)) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Log logAnnotation = signature.getMethod().getAnnotation(Log.class);

        // 获取或生成TraceID
        String traceId = TraceContext.getTraceId();

        long startTime = System.currentTimeMillis();
        String methodName = signature.getName();
        String classShortName = className.substring(className.lastIndexOf('.') + 1);

        // 构建日志信息
        StringBuilder logContent = new StringBuilder();
        logContent.append("\n================================================================================\n");
        logContent.append(String.format("[%s] TraceID: %s\n", dateFormat.format(new Date()), traceId));
        logContent.append(String.format("[%s] Class: %s\n", dateFormat.format(new Date()), classShortName));
        logContent.append(String.format("[%s] Method: %s\n", dateFormat.format(new Date()), methodName));

        Object result = null;
        Throwable exception = null;

        try {
            // 记录入参
            if (logAnnotation.params()) {
                Object[] args = joinPoint.getArgs();
                Parameter[] parameters = signature.getParameters();
                String paramsJson = getParamsJson(args, parameters);
                logContent.append(String.format("[%s] Params: %s\n", dateFormat.format(new Date()), paramsJson));
            }

            // 执行方法
            result = joinPoint.proceed();

            // 记录返回值
            if (logAnnotation.result() && result != null) {
                String resultJson = toJson(result);
                logContent.append(String.format("[%s] Result: %s\n", dateFormat.format(new Date()), resultJson));
            }

            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;

            // 记录耗时
            if (logAnnotation.time()) {
                logContent.append(String.format("[%s] Time: %d ms\n", dateFormat.format(new Date()), costTime));
            }

            // 记录异常
            if (logAnnotation.exception() && exception != null) {
                logContent.append(String.format("[%s] Exception: %s: %s\n",
                        dateFormat.format(new Date()),
                        exception.getClass().getSimpleName(),
                        exception.getMessage()));
            }

            logContent.append("================================================================================\n");

            // 写入日志
            logWriter.write(logContent.toString());
        }
    }

    private boolean isExcluded(String className) {
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(className).matches()) {
                return true;
            }
        }
        return false;
    }

    private String getParamsJson(Object[] args, Parameter[] parameters) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        List<Map<String, Object>> paramList = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Parameter param = parameters[i];

            // 检查是否标记了@LogIgnore
            if (param.isAnnotationPresent(LogIgnore.class)) {
                continue;
            }

            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("name", param.getName());
            paramMap.put("type", param.getType().getSimpleName());

            // 如果是对象，尝试过滤@LogIgnore字段
            if (arg != null && !isPrimitiveOrWrapper(arg.getClass())) {
                paramMap.put("value", filterIgnoredFields(arg));
            } else {
                paramMap.put("value", arg);
            }

            paramList.add(paramMap);
        }

        return toJson(paramList);
    }

    private Object filterIgnoredFields(Object obj) {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();
        Map<String, Object> result = new HashMap<>();

        // 处理Map类型
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!key.contains("password") && !key.contains("secret") && !key.contains("token")) {
                    result.put(key, entry.getValue());
                } else {
                    result.put(key, "***");
                }
            }
            return result;
        }

        // 处理普通对象
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(LogIgnore.class)) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(obj);

                // 过滤敏感字段
                String fieldName = field.getName();
                if (fieldName.toLowerCase().contains("password") ||
                        fieldName.toLowerCase().contains("secret") ||
                        fieldName.toLowerCase().contains("token")) {
                    result.put(fieldName, "***");
                } else {
                    result.put(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                log.debug("Failed to get field value: {}", field.getName());
            }
        }

        return result;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Boolean.class ||
                clazz == Character.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz == String.class ||
                clazz == Date.class;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
}