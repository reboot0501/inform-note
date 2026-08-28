# SQL 쿼리와 파라미터 바인딩 값 출력 방법 (P6Spy 가이드)

Spring Boot 4 + MyBatis 환경에서 실행 SQL 의 물음표(`?`) 파라미터를 **실제 값으로 치환한 완성형 SQL + 실행 시간(ms)** 을 콘솔에 출력하는 P6Spy 설정 방법입니다.

- 적용 모듈: `inform_note-boot`
- 관련 파일: `build.gradle`, `application.yaml`, `spy.properties`, `config/P6SpyConfig.java`

---

## 1. 개요 및 도입 배경

### 1.1. MyBatis 기본 로그의 한계
`org.apache.ibatis.logging.slf4j.Slf4jImpl` 로그는 SQL 과 파라미터가 분리됩니다.
```text
==>  Preparing: SELECT * FROM tb_down_event_log WHERE equipment_id = ? AND is_critical = ?
==> Parameters: EQP-101(String), true(Boolean)
```
- 파라미터가 많으면 DBeaver/DataGrip 재실행 시 `?` 자리에 값을 손으로 대입해야 함.
- 본 프로젝트는 `application.yaml` 에서 MyBatis `log-impl` 을 **주석 처리**(비활성)하고 P6Spy 로그만 사용합니다.

### 1.2. P6Spy 도입 효과
JDBC 레벨에서 SQL 을 가로채 **바인딩 완료된 온전한 SQL + 실행 시간(ms)** 을 출력합니다.
```text
----------------------------------------------------------------------------------------------------
[P6Spy SQL] Execution Time: 3 ms | Category: statement
----------------------------------------------------------------------------------------------------
SELECT * FROM tb_down_event_log WHERE equipment_id = 'EQP-101' AND is_critical = true
----------------------------------------------------------------------------------------------------
```

---

## 2. 프로젝트 적용 구성 요소

```
inform-note/
└── inform_note-boot/
    ├── build.gradle                         # p6spy-spring-boot-starter 의존성
    └── src/main/
        ├── java/io/nexcope/inform_note/config/
        │   └── P6SpyConfig.java             # @Configuration + MessageFormattingStrategy 구현
        └── resources/
            ├── application.yaml             # P6Spy Driver / URL, 로그 레벨, decorator 설정
            └── spy.properties               # appender / logMessageFormat 지정
```

---

## 3. 상세 구현 및 설정

### 3.1. Gradle 의존성 (`inform_note-boot/build.gradle`)

```groovy
dependencies {
    implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.10.0'
    runtimeOnly   'com.oracle.database.jdbc:ojdbc11'
}
```

### 3.2. DataSource 및 로깅 설정 (`application.yaml`)

본 프로젝트는 **P6Spy 표준 드라이버 방식** 을 사용합니다. (드라이버 자체를 `P6SpyDriver` 로, URL 접두어를 `jdbc:p6spy:` 로 지정)

```yaml
spring:
  application:
    name: inform-note
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:oracle:thin:@localhost:1521/FREEPDB1
    username: inform_note
    password: ...          # 실제 값은 커밋 금지 (환경변수/secret 권장)

logging:
  level:
    root: info
    io.nexcope.inform_note: info
    io.nexcope.inform_note.domain.log.mapper: info   # ※ 실제 매퍼 패키지는 domain.card.mapper (아래 NOTE)
    p6spy: info                                       # P6Spy 완성형 SQL 로그

# p6spy-spring-boot-starter (datasource-decorator) 설정
decorator:
  datasource:
    p6spy:
      enable-logging: true
      multiline: true
      logging: slf4j
```

> [!NOTE]
> - `logging.level` 아래 `io.nexcope.inform_note.domain.log.mapper` 항목은 **낡은 패키지명** 입니다. 실제 매퍼는 `io.nexcope.inform_note.domain.card.mapper.DownEventCardMapper` 이므로, MyBatis 분리 로그를 정밀 제어하려면 `io.nexcope.inform_note.domain.card.mapper` (또는 상위 `io.nexcope.inform_note.domain`) 로 바꾸는 것이 정확합니다. 다만 `log-impl` 이 비활성이라 현재 실질 영향은 없습니다.
> - **드라이버 방식(`P6SpyDriver`) 과 스타터 방식(`decorator.datasource.p6spy`) 이 동시에 설정** 되어 있습니다.
>   실제로 SQL 을 가로채는 것은 **드라이버 방식** 이며(URL 이 `jdbc:p6spy:...`), 스타터의 데코레이터 래핑은 이 경우 큰 역할을 하지 않습니다. 둘 중 하나만 유지해도 됩니다.
>   - 드라이버 방식만: `spy.properties` + `P6SpyDriver` + `jdbc:p6spy:` URL
>   - 스타터 방식만: 일반 Oracle 드라이버/URL + `decorator.datasource.p6spy.*` 로 포맷 제어

### 3.3. 사용자 정의 SQL 포맷터 (`P6SpyConfig.java`)

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
        // P6Spy 활성 인스턴스에 Slf4JLogger 와 이 클래스를 포맷터로 등록
        P6SpyOptions.getActiveInstance().setAppender(Slf4JLogger.class.getName());
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed,
                                String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";   // ResultSet 등 SQL 없는 이벤트는 로그 생략
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

> [!NOTE]
> - 이 클래스는 `@Configuration` 이면서 동시에 `MessageFormattingStrategy` 구현체입니다. Spring 빈으로 로드될 때 `@PostConstruct` 에서 P6Spy 에 **클래스명만** 등록하고(`setLogMessageFormat(getClass().getName())`), P6Spy 는 그 클래스명으로 **자체적으로 no-arg 인스턴스** 를 만들어 `formatMessage` 를 호출합니다. 따라서 포맷터 인스턴스에는 스프링 빈을 주입할 수 없습니다.
> - `formatMessage` 파라미터 중 `prepared`(? 형태 SQL), `now`, `connectionId`, `url` 은 현재 미사용입니다. 커넥션 ID/시각을 찍고 싶으면 여기에 추가하세요.
> - `category` 주요 값: `statement`(단건 실행), `batch`, `commit`, `rollback`, `resultset`, `result`.

### 3.4. `spy.properties` (`inform_note-boot/src/main/resources/`)

```properties
appender=com.p6spy.engine.spy.appender.Slf4JLogger
logMessageFormat=io.nexcope.inform_note.config.P6SpyConfig
```

> [!TIP]
> 로그 소음을 줄이려면 아래 옵션을 추가할 수 있습니다.
> ```properties
> # ResultSet/커밋/롤백 등 제외하고 SQL 실행만 로깅
> excludecategories=info,debug,result,resultset,batch,commit,rollback
> # 로그에 찍히는 시각 포맷
> dateformat=yyyy-MM-dd HH:mm:ss
> # 실행 시간 임계값(ms) 이상만 로깅 — 슬로우 쿼리 추적용
> # outagedetection=true
> # outagedetectioninterval=2
> ```

---

## 4. 실행 및 출력 예시

Swagger UI 등에서 `POST /feature/maintenance/find-down-event-cards/fetch` 호출 시
(`DownEventCardLogic.findOffsetElementListByCriteria` → `findByCriteria` + `countByCriteria` 2회 실행):

```text
----------------------------------------------------------------------------------------------------
[P6Spy SQL] Execution Time: 4 ms | Category: statement
----------------------------------------------------------------------------------------------------
SELECT down_event_id, equipment_id, ... , part_replacements, created_by, created_at, updated_by, updated_at
FROM tb_down_event_log
WHERE is_critical = 1
ORDER BY down_start_datetime DESC
OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY
----------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------
[P6Spy SQL] Execution Time: 2 ms | Category: statement
----------------------------------------------------------------------------------------------------
SELECT COUNT(*) FROM tb_down_event_log WHERE is_critical = 1
----------------------------------------------------------------------------------------------------
```

---

## 5. 핵심 이점

1. **완성형 쿼리 즉시 복사·실행**: `?` 대신 실제 값 → DB 툴에 바로 붙여넣기 가능.
2. **쿼리별 실행 시간(ms)**: 슬로우 쿼리 즉시 식별.
3. **드라이버 레벨 인터셉트**: MyBatis/JPA 등 상위 프레임워크와 무관하게 모든 JDBC 실행을 로깅.

---

## 6. 트러블슈팅

| 증상 | 원인 / 확인 |
|---|---|
| P6Spy 로그가 안 나옴 | `spy.properties` 가 classpath 루트(`src/main/resources`)에 있는지, `logMessageFormat` 클래스 경로가 정확한지 확인 |
| MyBatis `==> Preparing` 로그가 같이 나옴 | `application.yaml` 의 `mybatis.configuration.log-impl` 이 켜져 있지 않은지 확인 (본 프로젝트는 주석 처리) |
| 바인딩 값이 `?` 로 나옴 | 드라이버가 `P6SpyDriver` 인지, URL 이 `jdbc:p6spy:` 로 시작하는지 확인 |
| 로그가 너무 많음 | `spy.properties` 에 `excludecategories` 추가 (3.4 TIP) |
