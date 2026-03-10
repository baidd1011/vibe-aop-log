package com.vibelog.context;

import java.util.UUID;

/**
 * 请求TraceID上下文管理
 */
public class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 获取当前线程的TraceID
     */
    public static String getTraceId() {
        String traceId = TRACE_ID.get();
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    /**
     * 设置TraceID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 清除TraceID
     */
    public static void clear() {
        TRACE_ID.remove();
    }

    /**
     * 生成TraceID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取TraceID请求头名称
     */
    public static String getTraceIdHeader() {
        return TRACE_ID_HEADER;
    }
}