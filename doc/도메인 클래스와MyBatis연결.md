# DownEventLog 도메인 클래스와 MyBatis 연동 가이드

## 1. 개요
본 문서는 Oracle 23ai / 26ai의 **Native `JSON` 컬럼** 및 **Enum/Boolean** 컬럼을 포함하는 `tb_down_event_log` 테이블을 MyBatis 환경에서 번거로운 `<resultMap>` 태그 없이 **`resultType` 단독 지정만으로 `DownEventLog` 도메인 엔티티에 자동 매핑**하는 아키텍처와 구현 예시를 정리합니다.

---

## 2. 전체 아키텍처 구조

```
[ Oracle 23ai/26ai DB ]
   │
   ├─ Native JSON (OSON) : assigned_technician, approver, part_replacements
   ├─ Native BOOLEAN     : is_critical
   └─ Snake Case Columns : down_event_id, down_start_datetime ...
   │
   ▼ (MyBatis SQL Mapper)
   │ 
   ├─ map-underscore-to-camel-case: true (카멜케이스 자동 변환)
   └─ Generic JsonTypeHandler (Jackson 기반 자동 직렬화/역직렬화)
   │
   ▼
[ DownEventLog 도메인 엔티티 (POJO) ]
   ├─ AssignedTechnician (VO 객체)
   ├─ Approver (VO 객체)
   ├─ List<PartReplacement> (VO 리스트)
   └─ 비즈니스 메서드 (changeWorkStatus, completeAction 등)
```

---

## 3. Java 도메인 클래스 및 VO/Enum 정의

### 3.1. 메인 도메인 엔티티: `DownEventLog.java`
```java
package com.informnote.domain.downevent.domain;

import com.informnote.domain.downevent.domain.enums.*;
import com.informnote.domain.downevent.domain.vo.Approver;
import com.informnote.domain.downevent.domain.vo.AssignedTechnician;
import com.informnote.domain.downevent.domain.vo.PartReplacement;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 리플렉션 매핑을 위한 기본 생성자
@AllArgsConstructor
@Builder
public class DownEventLog {

    // [식별자]
    private String downEventId;
    private String equipmentId;
    private String chamberId;

    // [Enums]
    private FabricationPlant fabricationPlant;
    private ProcessModule processModule;
    private EquipmentModel equipmentModel;
    private DownType downType;
    private WorkStatus workStatus;

    // [일시 및 소요시간 (Epoch Milliseconds)]
    private Long downStartDatetime;
    private Long downEndDatetime;
    private Long downDurationMinutes;

    // [Oracle Native Boolean]
    @Builder.Default
    private Boolean isCritical = false;

    // [다운 코드 및 알람]
    private String downCode;
    private String downCodeDescription;
    private String alarmId;

    // [Native JSON 매핑 VO 객체 및 리스트]
    private AssignedTechnician assignedTechnician;
    private Approver approver;
    @Builder.Default
    private List<PartReplacement> partReplacements = new ArrayList<>();

    // [감사 메타]
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    // =================================================================
    // [비즈니스 메서드 (도메인 행위)]
    // =================================================================

    public void changeWorkStatus(WorkStatus newStatus, String modifierId) {
        this.workStatus = newStatus;
        this.updatedBy = modifierId;
    }

    public void completeAction(Long endEpochMs, String modifierId) {
        this.downEndDatetime = endEpochMs;
        this.workStatus = WorkStatus.ACTION_DONE;
        this.updatedBy = modifierId;

        if (this.downStartDatetime != null && endEpochMs != null) {
            long durationMs = endEpochMs - this.downStartDatetime;
            this.downDurationMinutes = Math.max(0, durationMs / (1000 * 60));
        }
    }

    public void assignTechnician(AssignedTechnician technician, String modifierId) {
        this.assignedTechnician = technician;
        this.workStatus = WorkStatus.IN_PROGRESS;
        this.updatedBy = modifierId;
    }
}
```

### 3.2. JSON 매핑용 Value Object (VO)
```java
package com.informnote.domain.downevent.domain.vo;

import lombok.*;

// 1. 담당 작업자 VO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignedTechnician {
    private String empNo;
    private String name;
    private String title;
    private Shift shift;
}

// 2. 승인자 VO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approver {
    private String empNo;
    private String name;
    private long approvedAt;
}

// 3. 교체 부품 VO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartReplacement {
    private String partNo;
    private String partName;
    private Integer qty;
}
```

### 3.3. Enum 정의
```java
package com.informnote.domain.downevent.domain.enums;

public enum FabricationPlant { FAB_1, FAB_2, FAB_3, RND_FAB }
public enum ProcessModule { PHOTO, ETCH, CVD, DIFFUSION, CMP, IMPLANT, CLEANS }
public enum EquipmentModel { ASML_NXT_1980DI, LAM_KIYO_CX, TEL_TRIAS, AMAT_CENTURA }
public enum DownType { HARDWARE, SOFTWARE, PROCESS, UTILITY, PM_MAINTENANCE }
public enum WorkStatus { DOWN_OCCURRED, IN_PROGRESS, ACTION_DONE, VERIFIED }
```

---

## 4. Jackson 기반 범용 `JsonTypeHandler` 구현

Oracle Native `JSON` 컬럼을 자바 객체(VO 및 `List<VO>`)로 양방향 자동 변환해주는 TypeHandler입니다.

```java
package com.informnote.global.typehandler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedJdbcTypes(JdbcType.OTHER)
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Class<T> type;

    public JsonTypeHandler(Class<T> type) {
        if (type == null) throw new IllegalArgumentException("Type argument cannot be null");
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 자바 객체 -> JSON 문자열 직렬화하여 DB에 전송
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("JSON Serialization Failed for " + type.getName(), e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toObject(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toObject(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toObject(cs.getString(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private T toObject(String content) throws SQLException {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try {
            // DB의 JSON 문자열 -> 자바 VO 객체로 역직렬화
            return objectMapper.readValue(content, type);
        } catch (Exception e) {
            throw new SQLException("JSON Deserialization Failed for " + type.getName() + " Content: " + content, e);
        }
    }
}
```

### 4.1. List 컬럼 전용 TypeHandler (`List<PartReplacement>`)
```java
package com.informnote.global.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.informnote.domain.downevent.domain.vo.PartReplacement;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@MappedTypes(List.class)
public class PartReplacementListTypeHandler extends BaseTypeHandler<List<PartReplacement>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<PartReplacement> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("JSON Serialization Error", e);
        }
    }

    @Override
    public List<PartReplacement> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getString(columnName));
    }

    @Override
    public List<PartReplacement> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getString(columnIndex));
    }

    @Override
    public List<PartReplacement> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getString(columnIndex));
    }

    private List<PartReplacement> toList(String content) throws SQLException {
        if (content == null || content.trim().isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(content, new TypeReference<List<PartReplacement>>() {});
        } catch (Exception e) {
            throw new SQLException("JSON Array Deserialization Error", e);
        }
    }
}
```

---

## 5. Spring Boot 환경 설정 (`application.yml`)

`application.yml`에 **스네이크케이스 자동 변환**과 **TypeHandler 패키지 경로**를 등록합니다.

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.informnote.domain.downevent.domain
  type-handlers-package: com.informnote.global.typehandler # TypeHandler 자동 스캔
  configuration:
    map-underscore-to-camel-case: true # down_event_id -> downEventId 자동 매핑
    default-fetch-size: 100
    default-statement-timeout: 30
```

---

## 6. MyBatis 매퍼 XML 및 Java Mapper 인터페이스

### 6.1. Java Mapper 인터페이스: `DownEventLogMapper.java`
```java
package com.informnote.domain.downevent.mapper;

import com.informnote.domain.downevent.domain.DownEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DownEventLogMapper {

    // 단건 조회
    Optional<DownEventLog> findById(@Param("downEventId") String downEventId);

    // 조건별 목록 조회
    List<DownEventLog> findAll();

    // 등록 (Insert)
    int insert(DownEventLog downEventLog);

    // 수정 (Update)
    int update(DownEventLog downEventLog);

    // 삭제 (Delete)
    int deleteById(@Param("downEventId") String downEventId);
}
```

### 6.2. 매퍼 XML: `DownEventLogMapper.xml`

JSON 컬럼과 일반 컬럼을 효율적으로 매핑하기 위해 **`autoMapping="true"` 기반의 `<resultMap>`**을 구성합니다.
- **일반 컬럼(식별자, Enum, 날짜, Long 등)**: `autoMapping="true"`에 의해 카멜케이스 규칙으로 자동 매핑됩니다.
- **JSON 컬럼(단일 객체 및 리스트)**: 단일 객체(`JsonTypeHandler`)와 리스트(`JsonListTypeHandler`) 간의 매핑 혼선을 방지하기 위해 해당 컬럼만 `<result>`로 명시합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.nexcope.inform_note.domain.log.mapper.DownEventLogMapper">

    <!-- ResultMap: 일반 컬럼은 autoMapping 처리, JSON 컬럼만 명시적 TypeHandler 지정 -->
    <resultMap id="downEventLogResultMap" type="io.nexcope.inform_note.domain.log.entity.DownEventLog" autoMapping="true">
        <!-- 단일 JSON 객체 매핑 -->
        <result property="assignedTechnician" column="assigned_technician" 
                javaType="io.nexcope.inform_note.domain.log.entity.vo.AssignedTechnician"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler" />
        <result property="approver" column="approver" 
                javaType="io.nexcope.inform_note.domain.log.entity.vo.Approver"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler" />
        <!-- 리스트 JSON 객체 매핑 -->
        <result property="partReplacements" column="part_replacements" 
                typeHandler="io.nexcope.inform_note.base.util.json.JsonListTypeHandler" />
    </resultMap>

    <!-- 1. 단건 조회 -->
    <select id="findById" parameterType="string" resultMap="downEventLogResultMap">
        SELECT 
            down_event_id,
            equipment_id,
            chamber_id,
            fabrication_plant,
            process_module,
            equipment_model,
            down_type,
            work_status,
            down_start_datetime,
            down_end_datetime,
            down_duration_minutes,
            is_critical,
            down_code,
            down_code_description,
            alarm_id,
            assigned_technician,
            approver,
            part_replacements,
            created_by,
            created_at,
            updated_by,
            updated_at
        FROM tb_down_event_log
        WHERE down_event_id = #{downEventId}
    </select>

    <!-- 2. 전체 목록 조회 -->
    <select id="findAll" resultMap="downEventLogResultMap">
        SELECT *
        FROM tb_down_event_log
        ORDER BY down_start_datetime DESC
    </select>


    <!-- 3. 등록 (Insert with JSON Objects) -->
    <insert id="insert" parameterType="com.informnote.domain.downevent.domain.DownEventLog">
        INSERT INTO tb_down_event_log (
            down_event_id,
            equipment_id,
            chamber_id,
            fabrication_plant,
            process_module,
            equipment_model,
            down_type,
            work_status,
            down_start_datetime,
            down_end_datetime,
            down_duration_minutes,
            is_critical,
            down_code,
            down_code_description,
            alarm_id,
            assigned_technician,
            approver,
            part_replacements,
            created_by,
            created_at,
            updated_by,
            updated_at
        ) VALUES (
            #{downEventId},
            #{equipmentId},
            #{chamberId},
            #{fabricationPlant},
            #{processModule},
            #{equipmentModel},
            #{downType},
            #{workStatus},
            #{downStartDatetime},
            #{downEndDatetime},
            #{downDurationMinutes},
            #{isCritical},
            #{downCode},
            #{downCodeDescription},
            #{alarmId},
            #{assignedTechnician, typeHandler=com.informnote.global.typehandler.JsonTypeHandler},
            #{approver, typeHandler=com.informnote.global.typehandler.JsonTypeHandler},
            #{partReplacements, typeHandler=com.informnote.global.typehandler.PartReplacementListTypeHandler},
            #{createdBy},
            SYSTIMESTAMP,
            #{updatedBy},
            SYSTIMESTAMP
        )
    </insert>

    <!-- 4. 수정 (Update) -->
    <update id="update" parameterType="com.informnote.domain.downevent.domain.DownEventLog">
        UPDATE tb_down_event_log
        SET 
            work_status             = #{workStatus},
            down_end_datetime       = #{downEndDatetime},
            down_duration_minutes   = #{downDurationMinutes},
            assigned_technician     = #{assignedTechnician, typeHandler=com.informnote.global.typehandler.JsonTypeHandler},
            approver                = #{approver, typeHandler=com.informnote.global.typehandler.JsonTypeHandler},
            part_replacements       = #{partReplacements, typeHandler=com.informnote.global.typehandler.PartReplacementListTypeHandler},
            updated_by              = #{updatedBy},
            updated_at              = SYSTIMESTAMP
        WHERE down_event_id = #{downEventId}
    </update>

    <!-- 5. 삭제 (Delete) -->
    <delete id="deleteById" parameterType="string">
        DELETE FROM tb_down_event_log
        WHERE down_event_id = #{downEventId}
    </delete>

</mapper>
```

---

## 7. 서비스 계층(Service) 사용 예시

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownEventLogService {

    private final DownEventLogMapper downEventLogMapper;

    // 조치 완료 비즈니스 로직 실행
    @Transactional
    public void completeAction(String downEventId, Long endEpochMs, String modifierId) {
        DownEventLog downEvent = downEventLogMapper.findById(downEventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + downEventId));

        // 도메인 메서드 호출 (상태 변경 및 다운 소요 시간 자동 계산)
        downEvent.completeAction(endEpochMs, modifierId);

        // 변경된 도메인 객체 업데이트
        downEventLogMapper.update(downEvent);
    }
}
```

---

## 8. `DownContent` (현상 및 조치 상세 본문) 도메인 및 MyBatis 매퍼 연동

`tb_down_content` 테이블의 대용량 HTML 본문(`CLOB`)을 관리하는 도메인 클래스와 매퍼 구성입니다.

### 8.1. 도메인 엔티티: `DownContent.java`
```java
package com.informnote.domain.downevent.domain;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class DownContent {

    private String downEventId;
    private String contentHtml;    // Froala 에디터 HTML 본문 (Oracle CLOB 매핑)
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    // =================================================================
    // [비즈니스 메서드 (도메인 행위)]
    // =================================================================

    /**
     * 본문 내용 수정
     */
    public void modifyContent(String newHtml, String modifierId) {
        this.contentHtml = (newHtml != null) ? newHtml : "";
        this.updatedBy = modifierId;
    }

    /**
     * 본문 존재 여부 확인
     */
    public boolean hasContent() {
        return this.contentHtml != null && !this.contentHtml.trim().isEmpty();
    }
}
```

---

### 8.2. Java Mapper 인터페이스: `DownContentMapper.java`
```java
package com.informnote.domain.downevent.mapper;

import com.informnote.domain.downevent.domain.DownContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface DownContentMapper {

    // 상세 본문 단건 조회
    Optional<DownContent> findById(@Param("downEventId") String downEventId);
    // 상세 본문 다건 조회 (IN 조건)
    List<DownContent> findByIds(@Param("downEventIds") List<String> downEventIds);
    // 상세 본문 등록 (Insert)
    int insert(DownContent downContent);
    // 상세 본문 수정 (Update)
    int update(DownContent downContent);
    // 상세 본문 등록 또는 수정 (Merge / Upsert)
    int saveOrUpdate(DownContent downContent);
    // 상세 본문 삭제 (Delete)
    int deleteById(@Param("downEventId") String downEventId);
}
```

---

### 8.3. 매퍼 XML: `DownContentMapper.xml`
`<resultMap>`을 작성하지 않고 **`resultType` 단독 지정**으로 CLOB 컬럼과 String 필드를 자동 매핑합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.informnote.domain.downevent.mapper.DownContentMapper">

    <!-- 1. 단건 조회 (resultType 사용 - CLOB은 자바 String에 자동 매핑됨) -->
    <select id="findById" parameterType="string" resultType="com.informnote.domain.downevent.domain.DownContent">
        SELECT 
            down_event_id,
            content_html,
            created_by,
            created_at,
            updated_by,
            updated_at
        FROM tb_down_content
        WHERE down_event_id = #{downEventId}
    </select>

    <select id="findByIds" resultType="io.nexcope.inform_note.domain.content.entity.DownContent">
        SELECT *
        FROM tb_down_content
        WHERE down_event_id IN
        <foreach item="id" collection="downEventIds" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

    <!-- 2. 신규 등록 (Insert) -->
    <insert id="insert" parameterType="com.informnote.domain.downevent.domain.DownContent">
        INSERT INTO tb_down_content (
            down_event_id,
            content_html,
            created_by,
            created_at,
            updated_by,
            updated_at
        ) VALUES (
            #{downEventId},
            #{contentHtml, jdbcType=CLOB},
            #{createdBy},
            SYSTIMESTAMP,
            #{updatedBy},
            SYSTIMESTAMP
        )
    </insert>

    <!-- 3. 본문 수정 (Update) -->
    <update id="update" parameterType="com.informnote.domain.downevent.domain.DownContent">
        UPDATE tb_down_content
        SET 
            content_html = #{contentHtml, jdbcType=CLOB},
            updated_by   = #{updatedBy},
            updated_at   = SYSTIMESTAMP
        WHERE down_event_id = #{downEventId}
    </update>

    <!-- 4. 등록 또는 수정 (Oracle MERGE INTO 구문) -->
    <insert id="saveOrUpdate" parameterType="com.informnote.domain.downevent.domain.DownContent">
        MERGE INTO tb_down_content target
        USING (SELECT #{downEventId} AS down_event_id FROM DUAL) src
           ON (target.down_event_id = src.down_event_id)
        WHEN MATCHED THEN
            UPDATE SET 
                target.content_html = #{contentHtml, jdbcType=CLOB},
                target.updated_by   = #{updatedBy},
                target.updated_at   = SYSTIMESTAMP
        WHEN NOT MATCHED THEN
            INSERT (
                down_event_id, 
                content_html, 
                created_by, 
                created_at, 
                updated_by, 
                updated_at
            ) VALUES (
                #{downEventId}, 
                #{contentHtml, jdbcType=CLOB}, 
                #{createdBy}, 
                SYSTIMESTAMP, 
                #{updatedBy}, 
                SYSTIMESTAMP
            )
    </insert>

    <!-- 5. 삭제 (Delete) -->
    <delete id="deleteById" parameterType="string">
        DELETE FROM tb_down_content
        WHERE down_event_id = #{downEventId}
    </delete>

</mapper>
```

---

---

## 9. MyBatis JSON 컬럼의 동적 Reflection TypeHandler 매핑 아키텍처

MyBatis에서 JSON 컬럼(`assigned_technician`, `approver`, `part_replacements`)을 도메인 엔티티의 VO 객체 및 제네릭 리스트(`List<T>`)로 매핑할 때, **컬럼마다 별도의 TypeHandler 클래스를 생성하지 않고** 범용 `JsonTypeHandler`와 `JsonListTypeHandler` 2개로 완전히 해결합니다.

### 9.1. 핵심 동작 원리
1. **단일 JSON 객체 매핑 (`JsonTypeHandler`)**:
   - DB 컬럼명 `assigned_technician` -> 자바 필드명 `assignedTechnician`으로 카멜케이스 변환
   - 도메인 엔티티 클래스를 리플렉션하여 `field.getType()`(`AssignedTechnician.class`)을 동적으로 추출 후 Jackson 역직렬화 수행
2. **JSON 배열/리스트 매핑 (`JsonListTypeHandler`)**:
   - DB 컬럼명 `part_replacements` -> 자바 필드명 `partReplacements`
   - 리플렉션을 통해 `ParameterizedType.getActualTypeArguments()[0]`를 추출하여 `List<T>`의 제네릭 실제 요소 타입(`PartReplacement.class`)을 100% 동적 판별
   - `JsonUtil.fromJsonList(json, targetClass)`를 호출하여 안전하게 `List<PartReplacement>`로 변환
3. **성능 최적화 (ConcurrentHashMap 캐싱)**:
   - 최초 1회 리플렉션으로 탐색된 타입 정보는 `TYPE_CACHE`에 캐싱되므로 이후 조회 시 리플렉션 오버헤드 없이 즉시 반환됩니다.

---

---

## 10. P6Spy를 활용한 실제 바인딩 SQL 및 실행 시간 로깅

MyBatis의 기본 로그(`==> Preparing: ...`, `==> Parameters: ...`)는 파라미터가 물음표(`?`)로 분리되어 있어 디버깅 및 쿼리 튜닝 시 번거롭습니다. 본 프로젝트에서는 **P6Spy**를 연동하여 실제 파라미터가 완성된 온전한 SQL과 실행 시간(ms)을 포맷팅하여 출력합니다.

### 10.1. 주요 설정 구성
1. **의존성 (`inform_note-boot/build.gradle`)**:
   ```groovy
   implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.10.0'
   ```
2. **DataSource 설정 (`application.yaml`)**:
   ```yaml
   spring:
     datasource:
       driver-class-name: com.p6spy.engine.spy.P6SpyDriver
       url: jdbc:p6spy:oracle:thin:@localhost:1521/FREEPDB1
   ```
3. **포맷터 클래스 (`P6SpyConfig.java`)**:
   - `MessageFormattingStrategy`를 구현하고 `@PostConstruct`에서 `Slf4JLogger` 및 포맷터 클래스를 P6Spy에 등록
4. **P6Spy 설정 (`spy.properties`)**:
   ```properties
   appender=com.p6spy.engine.spy.appender.Slf4JLogger
   logMessageFormat=io.nexcope.inform_note.config.P6SpyConfig
   ```

---

## 11. 요약 및 핵심 이점
1. **보일러플레이트 제거**: 컬럼별 TypeHandler 클래스를 일일이 만들 필요 없이 범용 `JsonTypeHandler` / `JsonListTypeHandler` 2개로 모든 JSON 컬럼 처리 완료.
2. **동적 제네릭 타입 자동 추론 (Reflection)**: `List<T>`의 실제 제네릭 요소 타입을 리플렉션으로 자동 추출하므로, 향후 새로운 JSON 리스트 컬럼이 추가되어도 코드 수정 없이 100% 자동 동작.
3. **Oracle 23ai Native JSON & CLOB 지원**: Jackson 기반 직렬화/역직렬화로 자바 VO 객체 및 List와 OSON 컬럼 간 자동 변환.
4. **P6Spy 기반 파라미터 완성형 SQL 로깅**: `?` 대신 실제 바인딩된 온전한 SQL과 실행 시간을 가독성 높은 포맷으로 콘솔에 출력.
5. **Rich Domain Model 유지**: 무미건조한 DTO 대신 비즈니스 행위(메서드)를 가진 도메인 객체를 MyBatis 반환값으로 직접 사용하여 서비스 로직의 응집도 극대화.



