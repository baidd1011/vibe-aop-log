package com.vibelog.writer;

import com.vibelog.config.LogProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 文件日志写入器实现
 * 支持按日期滚动和文件大小限制
 */
@Slf4j
public class FileLogWriter implements LogWriter {

    private final LogProperties properties;
    private final SimpleDateFormat dateFormat;

    private final AtomicReference<FileWriter> currentWriter = new AtomicReference<>();
    private final AtomicReference<String> currentDate = new AtomicReference<>();

    private long maxFileSize;
    private File logDir;

    public FileLogWriter(LogProperties properties) {
        this.properties = properties;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        init();
    }

    private void init() {
        // 解析文件大小
        String maxSize = properties.getMaxFileSize();
        this.maxFileSize = parseFileSize(maxSize);

        // 创建日志目录
        this.logDir = new File(properties.getLogDir());
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // 初始化当前日期
        currentDate.set(dateFormat.format(new Date()));

        // 清理历史文件
        cleanOldFiles();
    }

    @Override
    public synchronized void write(String content) {
        try {
            FileWriter writer = getWriter();
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to write log: {}", e.getMessage(), e);
        }
    }

    private FileWriter getWriter() throws IOException {
        String today = dateFormat.format(new Date());

        // 日期变化，创建新文件
        if (!today.equals(currentDate.get())) {
            closeCurrentWriter();
            currentDate.set(today);
        }

        FileWriter writer = currentWriter.get();
        if (writer == null) {
            File logFile = getLogFile(today);
            writer = new FileWriter(logFile, true);
            currentWriter.set(writer);
        }

        // 检查文件大小，超过限制则滚动
        File logFile = getLogFile(today);
        if (logFile.length() >= maxFileSize) {
            closeCurrentWriter();
            // 添加序号
            writer = new FileWriter(getRollingFileName(today), true);
            currentWriter.set(writer);
        }

        return writer;
    }

    private File getLogFile(String date) {
        return new File(logDir, properties.getFileName());
    }

    private File getRollingFileName(String date) {
        String fileName = properties.getFileName();
        int dotIndex = fileName.lastIndexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String ext = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        int counter = 1;
        File rollingFile;
        do {
            rollingFile = new File(logDir, name + "." + date + "." + counter + ext);
            counter++;
        } while (rollingFile.exists());

        return rollingFile;
    }

    private void closeCurrentWriter() {
        FileWriter writer = currentWriter.getAndSet(null);
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.error("Failed to close writer: {}", e.getMessage(), e);
            }
        }
    }

    private void cleanOldFiles() {
        File[] files = logDir.listFiles((dir, name) -> {
            String fileName = properties.getFileName();
            int dotIndex = fileName.lastIndexOf('.');
            String nameWithoutExt = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
            return name.startsWith(nameWithoutExt);
        });

        if (files == null || files.length == 0) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - (properties.getMaxHistory() * 24L * 60 * 60 * 1000);

        for (File file : files) {
            if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    log.info("Deleted old log file: {}", file.getName());
                }
            }
        }
    }

    @Override
    public void flush() {
        FileWriter writer = currentWriter.get();
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                log.error("Failed to flush writer: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() {
        closeCurrentWriter();
    }

    private long parseFileSize(String size) {
        size = size.toUpperCase().trim();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2).trim()) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2).trim()) * 1024;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.substring(0, size.length() - 2).trim()) * 1024 * 1024 * 1024;
        } else if (size.endsWith("B")) {
            return Long.parseLong(size.substring(0, size.length() - 1).trim());
        }
        return Long.parseLong(size);
    }
}