# Vibe AOP Log

一个轻量级、易集成的 AOP 日志框架，为 Spring Boot 项目提供方法级别的日志记录能力。

## 特性

- **简单易用**：通过注解即可使用，无需修改业务代码
- **灵活配置**：支持记录入参、返回值、执行耗时、异常信息
- **敏感信息保护**：支持忽略敏感字段，自动过滤密码、token 等
- **TraceID 支持**：自动生成和传递请求链路 ID
- **文件滚动**：支持按日期、大小滚动，自动清理历史文件
- **Spring Boot 兼容**：支持 Spring Boot 2.x 和 3.x

## 快速开始

### 1. 引入依赖

**Maven:**

```xml
<dependency>
    <groupId>com.vibelog</groupId>
    <artifactId>vibe-aop-log</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'com.vibelog:vibe-aop-log:1.0.0'
```

### 2. 配置（可选）

在 `application.yml` 中添加配置：

```yaml
vibe:
  log:
    enabled: true                    # 是否启用（默认true）
    log-dir: logs/vibe-log          # 日志目录
    file-name: method.log           # 日志文件名
    max-file-size: 10MB             # 单个文件最大大小
    max-history: 30                 # 保留历史天数
    exclude-classes:                # 排除的类（支持通配符）
      - com.example.exclude.*
    params: true                    # 默认记录入参
    result: true                    # 默认记录返回值
    time: true                      # 默认记录耗时
    exception: true                 # 默认记录异常
```

### 3. 使用注解

#### 基本用法

```java
import com.vibelog.annotation.Log;

@Service
public class UserService {

    @Log
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
```

#### 高级用法

```java
import com.vibelog.annotation.Log;
import com.vibelog.annotation.LogIgnore;

// 记录指定内容
@Log(value = "创建订单", params = true, result = true, time = true, exception = true)
public Order createOrder(OrderDTO dto) {
    return orderMapper.insert(dto);
}

// 忽略敏感字段
@Log
public void saveUser(@LogIgnore User user) {
    // user对象的password字段不会被记录
    userMapper.insert(user);
}
```

## 注解说明

### @Log

标记在方法上，用于启用日志记录。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| value | String | "" | 日志描述 |
| params | boolean | true | 是否记录入参 |
| result | boolean | true | 是否记录返回值 |
| time | boolean | true | 是否记录执行耗时 |
| exception | boolean | true | 是否记录异常信息 |

### @LogIgnore

标记在方法参数或类的字段上，用于忽略该参数的记录。

```java
// 标记在参数上
@Log
public void saveUser(@LogIgnore User user) {
    // 整个user对象都不会被记录
}

// 标记在字段上
public class User {
    private Long id;
    private String name;

    @LogIgnore
    private String password;  // password字段不会被记录
}
```

## 日志输出示例

```
================================================================================
[2024-01-15 10:30:25.123] TraceID: a1b2c3d4e5f6g7h8
[2024-01-15 10:30:25.123] Class: UserService
[2024-01-15 10:30:25.123] Method: getUserById
[2024-01-15 10:30:25.123] Params: [{"name":"id","type":"Long","value":123}]
[2024-01-15 10:30:25.456] Result: {"id":123,"name":"张三","email":"zhangsan@example.com"}
[2024-01-15 10:30:25.456] Time: 333 ms
================================================================================
```

## TraceID 使用

框架会自动为每个请求生成唯一的 TraceID，用于链路追踪。

### 获取 TraceID

```java
import com.vibelog.context.TraceContext;

// 获取当前线程的TraceID
String traceId = TraceContext.getTraceId();

// 设置自定义TraceID
TraceContext.setTraceId("custom-trace-id");

// 清除TraceID
TraceContext.clear();
```

### MDC 集成（可选）

如果你使用 MDC 进行日志打印，可以在配置中添加：

```java
// 在日志切面中添加
MDC.put("traceId", TraceContext.getTraceId());
```

## 扩展日志写入器

框架提供了 `LogWriter` 接口，支持自定义日志输出方式。

### 实现自定义写入器

```java
import com.vibelog.writer.LogWriter;

public class ConsoleLogWriter implements LogWriter {

    @Override
    public void write(String content) {
        System.out.print(content);
    }

    @Override
    public void flush() {
        System.out.flush();
    }

    @Override
    public void close() {
        // 清理资源
    }
}
```

### 注册自定义写入器

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfig {

    @Bean
    public LogWriter customLogWriter() {
        return new ConsoleLogWriter();
    }
}
```

## 排除指定类

可以通过配置排除某些类不进行日志记录：

```yaml
vibe:
  log:
    exclude-classes:
      - com.example.controller.*
      - com.example.service.internal.*
```

支持通配符：
- `*` 匹配任意字符
- `**` 匹配任意路径

## 注意事项

1. **性能考虑**：大量日志写入可能影响性能，建议仅在关键方法上使用 `@Log` 注解
2. **敏感数据**：建议在密码、token 等敏感字段上使用 `@LogIgnore`
3. **日志清理**：根据 `max-history` 配置定期清理历史日志文件
4. **线程安全**：框架本身是线程安全的，可以在并发环境下使用

## 项目结构

```
vibe-aop-log/
├── pom.xml
├── src/main/java/com/vibelog/
│   ├── VibeLogAutoConfiguration.java    # 自动装配类
│   ├── annotation/
│   │   ├── Log.java                     # 主注解
│   │   └── LogIgnore.java               # 忽略参数注解
│   ├── aspect/
│   │   └── LogAspect.java               # AOP 切面实现
│   ├── config/
│   │   └── LogProperties.java           # 配置属性类
│   ├── context/
│   │   └── TraceContext.java            # TraceID 上下文
│   └── writer/
│       ├── LogWriter.java               # 日志写入器接口
│       └── FileLogWriter.java           # 文件输出实现
└── src/main/resources/
    └── META-INF/
        ├── spring.factories             # Spring Boot 2.x
        └── spring/xxx.imports           # Spring Boot 3.x
```

## 许可证

MIT License