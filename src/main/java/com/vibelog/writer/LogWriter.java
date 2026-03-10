package com.vibelog.writer;

/**
 * 日志写入器接口
 */
public interface LogWriter {

    /**
     * 写入日志
     *
     * @param content 日志内容
     */
    void write(String content);

    /**
     * 刷新缓冲区
     */
    void flush();

    /**
     * 关闭写入器
     */
    void close();
}