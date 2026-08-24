# SQL 쿼리와 파라미터 바인딩 값 출력 방법 (P6Spy 가이드)

본 문서는 Spring Boot + MyBatis 환경에서 실행되는 SQL 쿼리의 물음표(`?`) 파라미터 바인딩 값을 실제 값으로 치환하여 **완성형 SQL 및 쿼리 실행 시간(ms)을 가독성 높은 포맷으로 콘솔에 출력하는 P6Spy 설정 및 구현 방법**을 정리합니다.

---

## 1. 개요 및 도입 배경

### 1.1. MyBatis 기본 로그의 한계
MyBatis의 기본 로깅(`org.apache.ibatis.logging.slf4j.Slf4jImpl`)은 아래와 같이 SQL과 파라미터가 분리되어 출력됩니다:
```text
==>  Preparing: SELECT * FROM tb_down_event_log WHERE equipment_id = ? AND is_critical = ?
==> Parameters: EQP-101(String), true(Boolean)
```
- 파라미터가 많은 복잡한 쿼리의 경우, SQL 툴(DBeaver, DataGrip 등)에서 쿼리를 재실행하거나 디버깅할 때 일일이 `?` 자리에 값을 손으로 대입해야 하는 큰 불편함이 발생합니다.

### 1.2. P6Spy 도입 효과
P6Spy는 JDBC 레벨에서 실행되는 SQL을 가로채어 **파라미터가 완벽히 바인딩된 온전한 SQL과 실행 시간(ms)**을 출력해 줍니다:
```text
----------------------------------------------------------------------------------------------------
[P6Spy SQL] Execution Time: 3 ms | Category: statement
----------------------------------------------------------------------------------------------------
SELECT * FROM tb_down_event_log WHERE equipment_id = 'EQP-101' AND is_critical = true
----------------------------------------------------------------------------------------------------
```

---

## 2. 프로젝트 적용 전체 구성 요소

```
inform-note
  │
  ├── inform_note-boot
  │     │
  │     ├── build.gradle (의존성 추가)
  │     ├── src/main/resources
  │     │     ├── application.yaml (P6Spy Driver 및 로그 레벨 설정)
  │     │     └── spy.properties (P6Spy Appender 및 포맷터 지정)
  │     │
  │     └── src/main/java/io/nexcope/inform_note/config
  │           └── P6SpyConfig.java (사용자 정의 SQL 포맷터 구현체)
```

---

## 3. 상세 구현 및 설정

### 3.1. Gradle 의존성 추가 (`inform_note-boot/build.gradle`)

```groovy
dependencies {
    // P6Spy Spring Boot Starter
    implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.10.0'
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11'
}
```

---

### 3.2. DataSource 및 로깅 설정 (`application.yaml`)

Spring Boot 3/4 환경에서 데코레이터 프록시 누락 문제를 방지하고 100% 확실하게 동작하도록 **P6Spy 표준 드라이버**를 설정합니다:

```yaml
spring:
  # database 접속 정보 (P6Spy Driver 적용)
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:oracle:thin:@localhost:1521/FREEPDB1
    username: inform_note
    password: ...

# 로깅 레벨 설정
logging:
  level:
    root: info
    io.nexcope.inform_note: info
    io.nexcope.inform_note.domain.card.mapper: info  # MyBatis 기본 분리 로그(? 출력) 비활성화
    p6spy: info                                    # P6Spy 바인딩 완성형 SQL 로그 활성화

# P6Spy 데코레이터 설정
decorator:
  datasource:
    p6spy:
      enable-logging: true
      multiline: true
      logging: slf4j
```

---

### 3.3. 사용자 정의 SQL 포맷터 (`P6SpyConfig.java`)

`MessageFormattingStrategy` 인터페이스를 구현하여 쿼리 실행 시간, 카테고리, 포맷팅된 SQL을 깔끔한 구분선과 함께 출력하도록 구현합니다:

```java
package io.nexcope.inform_note.config;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyConfig implements MessageFormattingStrategy {

    @PostConstruct
    public void setLogMessageFormat() {
        // Slf4JLogger 및 포맷터 클래스를 P6Spy 활성 인스턴스에 등록
        P6SpyOptions.getActiveInstance().setAppender(Slf4JLogger.class.getName());
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n----------------------------------------------------------------------------------------------------");
        sb.append("\n[P6Spy SQL] Execution Time: ").append(elapsed).append(" ms | Category: ").append(category);
        sb.append("\n----------------------------------------------------------------------------------------------------");
        sb.append("\n").append(sql.trim());
        sb.append("\n----------------------------------------------------------------------------------------------------");

        return sb.toString();
    }
}
```

---

### 3.4. P6Spy 설정 파일 (`spy.properties`)

`inform_note-boot/src/main/resources/spy.properties`에 기본 Appender와 포맷터 클래스를 지정합니다:

```properties
appender=com.p6spy.engine.spy.appender.Slf4JLogger
logMessageFormat=io.nexcope.inform_note.config.P6SpyConfig
```

---

## 4. 실행 및 출력 결과 예시

Swagger UI 또는 클라이언트에서 `findDownEvents` API를 호출하면 콘솔에 다음과 같이 출력됩니다:

```text
----------------------------------------------------------------------------------------------------
[P6Spy SQL] Execution Time: 4 ms | Category: statement
----------------------------------------------------------------------------------------------------
SELECT * FROM tb_down_event_log ORDER BY down_start_datetime DESC OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
[P6Spy SQL] Execution Time: 2 ms | Category: statement
----------------------------------------------------------------------------------------------------
SELECT COUNT(*) FROM tb_down_event_log
----------------------------------------------------------------------------------------------------
```

---

## 5. 핵심 이점 요약

1. **완성형 쿼리 즉시 복사 & 실행**: 파라미터가 물음표(`?`) 대신 실제 값으로 치환되어 출력되므로 DB 툴에서 즉시 붙여넣어 실행/검증 가능.
2. **실시간 쿼리 성능 모니터링**: 쿼리별 소요 시간(`Execution Time: X ms`)이 함께 출력되어 슬로우 쿼리 즉시 식별 가능.
3. **독립적이고 안전한 설정**: P6Spy 표준 드라이버(`P6SpyDriver`) 방식을 적용하여 Spring Boot 버전 업그레이드 시에도 안정적으로 동작.
