# DownEventCard 도메인 클래스와 MyBatis 연동 가이드

## 1. 개요

Oracle 23ai/26ai 의 **Native `JSON` 컬럼** 및 **Native `BOOLEAN` / Enum / Epoch(Number)** 컬럼을 포함하는 `tb_down_event_log`, `tb_down_content` 테이블을,
MyBatis 에서 **`autoMapping` 기반 최소 `<resultMap>` (또는 `resultType` 단독)** 으로 도메인 엔티티(`DownEventCard`, `DownContent`)에 매핑하는 실제 구현을 정리합니다.

- 모듈/패키지: `inform_note-domain` / `io.nexcope.inform_note.domain.{card,content}`
- 테이블: `tb_down_event_log` (엔티티 `DownEventCard`), `tb_down_content` (엔티티 `DownContent`)
- 공통 TypeHandler: `inform_note-base` / `io.nexcope.inform_note.base.util.json` (상세: [`Mybastis Json 컬럼의 json handler 처리.md`](./Mybastis%20Json%20컬럼의%20json%20handler%20처리.md))

> [!IMPORTANT]
> 문서 초안에서 사용하던 `DownEventLog` / `com.informnote.domain.downevent.*` 표기는 **현행이 아닙니다.**
> - 클래스명: **`DownEventCard`**, 패키지: **`io.nexcope.inform_note.domain.card.entity`**
> - 테이블명만 레거시로 **`tb_down_event_log`** 유지
> - TypeHandler 패키지: **`io.nexcope.inform_note.base.util.json`** (`com.informnote.global.typehandler` 아님)

---

## 2. 전체 아키텍처

```
[ Oracle 23ai/26ai ]
   ├─ Native JSON      : assigned_technician, approver, part_replacements
   ├─ Native BOOLEAN   : is_critical
   ├─ NUMBER(19)       : down_start_datetime / down_end_datetime / down_duration_minutes  (Epoch ms → Long)
   ├─ TIMESTAMP WITH TIME ZONE : created_at / updated_at  (→ OffsetDateTime)
   ├─ CLOB             : tb_down_content.content_html  (→ String)
   └─ snake_case 컬럼   : down_event_id, down_code ...
        │  (MyBatis SQL Mapper)
        ├─ map-underscore-to-camel-case: true    → 일반 컬럼 자동 카멜케이스 매핑
        ├─ default-enum-type-handler: AutoEnumTypeHandler → 모든 Enum 컬럼 유연 매핑
        └─ JsonTypeHandler / JsonListTypeHandler → JSON 컬럼 ↔ VO / List<VO>
        ▼
[ DownEventCard / DownContent 도메인 엔티티 (Rich Domain) ]
   ├─ AssignedTechnician, Approver, List<PartReplacement>
   └─ 비즈니스 메서드: changeWorkStatus / completeAction / assignTechnician / specifyReplacement ...
```

---

## 3. Java 도메인 클래스 (실제)

### 3.1. 메인 엔티티: `DownEventCard.java`

```java
package io.nexcope.inform_note.domain.card.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.card.entity.dto.DownEventCardDto;
import io.nexcope.inform_note.domain.card.entity.vo.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 리플렉션 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class DownEventCard {
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

    // [Oracle Native BOOLEAN] — primitive boolean (기본값/래퍼 아님)
    private boolean isCritical;

    // [다운 코드 및 알람]
    private String downCode;
    private String downCodeDescription;
    private String alarmId;

    // [Native JSON 매핑 VO / 리스트]
    private AssignedTechnician assignedTechnician;
    private Approver approver;
    @Builder.Default
    private List<PartReplacement> partReplacements = new ArrayList<>();

    // [감사 메타]
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    // DTO 기반 신규 생성 (ID 생성 + 감사 시각 세팅)
    public DownEventCard(DownEventCardDto dto) {
        this.downEventId = dto.genId();
        BeanUtils.copyProperties(dto, this);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // downEventId 규칙: {equipmentId}_D{yy-MM-dd_HH:mm:ss.SSS}
    public static String genId(String equipmentId) {
        OffsetDateTime now = OffsetDateTime.now();
        return String.format("%s_D%s", equipmentId,
                now.format(DateTimeFormatter.ofPattern("yy-MM-dd_HH:mm:ss.SSS")));
    }

    public static DownEventCard fromJson(String json) {
        return JsonUtil.fromJson(json, DownEventCard.class);
    }

    // ===== 비즈니스 메서드 =====
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

    public void specifyReplacement(List<PartReplacement> partReplacements, String modifierId) {
        this.partReplacements = partReplacements;
        this.updatedBy = modifierId;
    }
}
```

### 3.2. JSON 매핑용 Value Object (실제 필드)

```java
// io.nexcope.inform_note.domain.card.entity.vo

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignedTechnician {
    private String empNo;      // tb_employees.emp_no
    private String name;       // tb_employees.name
    private String jobTitle;   // tb_jobs.job_title   ※ 초안의 'title' 아님
    private Shift  shift;       // A / B / C
}

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Approver {
    private String empNo;
    private String JobTitle;    // ※ 필드명이 대문자 'J' 시작 → JSON 키도 "JobTitle"
    private String name;
    private long   approvedAt;  // Epoch
}

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartReplacement {
    private ReplacementType replacementType;  // REPLACEMENT_PART / USE_MATERIAL
    private String  partNo;
    private String  partName;
    private Integer qty;
}
```

### 3.3. Enum 정의 (실제 상수)

```java
// io.nexcope.inform_note.domain.card.entity.vo — 각 Enum 은 description + getCodeNames() 보유

public enum FabricationPlant { FAB_1, FAB_2, FAB_3, RND_FAB, PKG }

public enum ProcessModule    { PHOTO, ETCH, DIFF, CMP, CVD, IMP, METAL }

public enum EquipmentModel {   // Photo/Etch/CVD/CMP/Diffusion/Implant/Cleans/Metrology 약 30종
    ASML_NXT_1980DI, NIKON_NSR_S620D, ASML_TWINSCAN, CANON_FPA_6300ES6A, TEL_LITHIUS_PRO_i,
    LAM_KIYO_CX, TEL_TACTRAS, AMAT_CENTRIS_SYM3, LAM_VERSYS,
    TEL_TRIAS, AMAT_PRODUCER, ASM_EAGLE_12, JUSUNG_EUREKA, AMAT_ENDURE_CLOVER,
    AMAT_REFLEXION, EBARA_F_REX, KC_TECH_CMP_300,
    KOKUSAI_ADVANCED, TEL_ALPHA, ASM_A412,
    VARIAN_VIISTA, AXCELIS_PURION_H, AMAT_VIISTA_TRIDENT,
    SEMES_IRIS, DNS_SS_3000, TEL_CELLESSE, PSK_SUPRA, KLA_2925
}

public enum DownType   { HARDWARE, SOFTWARE, PROCESS, UTILITY, OPTICAL, CONSUMABLE, PREVENTIVE, OPERATOR }

public enum WorkStatus { DOWN_OCCURRED, IN_PROGRESS, ACTION_DONE, VERIFIED, CLOSED }

public enum Shift      { A, B, C }

public enum ReplacementType { REPLACEMENT_PART, USE_MATERIAL }
```

> DB 에는 Enum **상수명**(`FAB_1`, `ACTION_DONE` 등)이 저장됩니다. `AutoEnumTypeHandler` 가 `FAB-1`, `PM MAINTENANCE` 같이 하이픈/공백이 섞인 값도 언더스코어로 치환해 재매칭합니다.

---

## 4. JSON 컬럼 TypeHandler (요약)

`inform_note-base` 의 범용 핸들러 **2개** 만 사용합니다. 컬럼별 핸들러(`PartReplacementListTypeHandler` 등)는 **만들지 않습니다.**

| 핸들러 | 대상 | 타입 해석 방식 |
|---|---|---|
| `JsonTypeHandler` | 단일 VO (`assigned_technician`, `approver`) | `javaType` 지정 시 그대로 / no-arg 등록 시 도메인 `entity` 패키지 **classpath 자동 스캔** 으로 컬럼→필드타입 해석 |
| `JsonListTypeHandler` | `List<VO>` (`part_replacements`) | 컬럼명→필드명 리플렉션으로 `List<T>` 요소 타입 해석 (⚠️ 후보 클래스명 낡음 — 아래 NOTE) |

```java
// application.yaml 발췌
mybatis:
  type-aliases-package: io.nexcope.inform_note.domain
  type-handlers-package: io.nexcope.inform_note.base.util.json   // ← 3종 핸들러 no-arg 자동 등록
  configuration:
    map-underscore-to-camel-case: true
    call-setters-on-nulls: true
    jdbc-type-for-null: varchar
    default-enum-type-handler: io.nexcope.inform_note.base.util.json.AutoEnumTypeHandler
    auto-mapping-unknown-column-behavior: none
    default-statement-timeout: 30
```

> [!NOTE]
> `JsonListTypeHandler` 의 하드코딩 후보 배열이 `io.nexcope.inform_note.domain.log.entity.DownEventLog` (존재하지 않음)를 참조합니다.
> `part_replacements` 요소 타입 해석이 `PartReplacement` 로 안전하게 잡히도록 하려면 **XML `<result>` / 인라인 `#{}` 에 `javaType` 을 명시** 하거나 핸들러를 자동 스캔 방식으로 통일하세요. (상세: JSON 핸들러 문서 3.2)

---

## 5. MyBatis 매퍼 (실제)

### 5.1. `DownEventCardMapper.java`

```java
package io.nexcope.inform_note.domain.card.mapper;

import io.nexcope.inform_note.domain.card.entity.DownEventCard;
import io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DownEventCardMapper {
    Optional<DownEventCard> findById(@Param("downEventId") String downEventId);
    List<DownEventCard> findAll();
    List<DownEventCard> findByCriteria(DownEventCardSearchCriteria criteria);   // 페이징 목록
    long countByCriteria(DownEventCardSearchCriteria criteria);                 // 페이징 카운트
    int insert(DownEventCard downEventCard);
    int update(DownEventCard downEventCard);
    int deleteById(@Param("downEventId") String downEventId);
}
```

> - `@Mapper` 어노테이션 + `mybatis-spring-boot-starter` 자동 스캔으로 등록됩니다(별도 `@MapperScan` 없음).
> - 초안의 `findAll()` 만 있는 형태가 아니라 **`findByCriteria` / `countByCriteria`** 가 실제 조회의 핵심입니다.

### 5.2. `DownEventCardMapper.xml` (실제 구조)

```xml
<mapper namespace="io.nexcope.inform_note.domain.card.mapper.DownEventCardMapper">

    <!-- 일반 컬럼 autoMapping, JSON 3개만 명시 -->
    <resultMap id="downEventCardResultMap"
               type="io.nexcope.inform_note.domain.card.entity.DownEventCard" autoMapping="true">
        <result property="assignedTechnician" column="assigned_technician"
                javaType="io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler"/>
        <result property="approver" column="approver"
                javaType="io.nexcope.inform_note.domain.card.entity.vo.Approver"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler"/>
        <result property="partReplacements" column="part_replacements"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonListTypeHandler"/>
    </resultMap>

    <sql id="searchConditions">
        <where>
            <if test="fabricationPlant != null"> AND fabrication_plant = #{fabricationPlant} </if>
            <if test="processModule != null">    AND process_module    = #{processModule} </if>
            <if test="equipmentModel != null">   AND equipment_model   = #{equipmentModel} </if>
            <if test="equipmentId != null and equipmentId != ''"> AND equipment_id = #{equipmentId} </if>
            <if test="keyword != null and keyword != ''">
                AND ( UPPER(down_code) LIKE UPPER('%' || #{keyword} || '%')
                   OR UPPER(alarm_id)  LIKE UPPER('%' || #{keyword} || '%')
                   OR UPPER(down_code_description) LIKE UPPER('%' || #{keyword} || '%') )
            </if>
            <if test="isCritical != null"> AND is_critical = #{isCritical} </if>
            <if test="downType != null">   AND down_type   = #{downType} </if>
            <if test="workStatus != null"> AND work_status = #{workStatus} </if>
            <if test="shift != null">
                AND JSON_VALUE(assigned_technician, '$.shift') = #{shift}
            </if>
            <if test="technician != null and technician != ''">
                AND ( JSON_VALUE(assigned_technician, '$.name')  LIKE '%' || #{technician} || '%'
                   OR JSON_VALUE(assigned_technician, '$.empNo') = #{technician} )
            </if>
            <if test="downStartDatetimeFrom != null"> AND down_start_datetime &gt;= #{downStartDatetimeFrom} </if>
            <if test="downStartDatetimeTo != null">   AND down_start_datetime &lt;= #{downStartDatetimeTo} </if>
        </where>
    </sql>

    <select id="findById" parameterType="string" resultMap="downEventCardResultMap">
        SELECT down_event_id, equipment_id, chamber_id, fabrication_plant, process_module,
               equipment_model, down_type, work_status, down_start_datetime, down_end_datetime,
               down_duration_minutes, is_critical, down_code, down_code_description, alarm_id,
               assigned_technician, approver, part_replacements,
               created_by, created_at, updated_by, updated_at
        FROM tb_down_event_log
        WHERE down_event_id = #{downEventId}
    </select>

    <select id="findByCriteria"
            parameterType="io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria"
            resultMap="downEventCardResultMap">
        SELECT /* 위 컬럼 목록 동일 */ *
        FROM tb_down_event_log
        <include refid="searchConditions"/>
        ORDER BY down_start_datetime DESC
        OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY
    </select>

    <select id="countByCriteria"
            parameterType="io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria"
            resultType="long">
        SELECT COUNT(*) FROM tb_down_event_log
        <include refid="searchConditions"/>
    </select>

    <insert id="insert" parameterType="io.nexcope.inform_note.domain.card.entity.DownEventCard">
        INSERT INTO tb_down_event_log ( down_event_id, ... , assigned_technician, approver, part_replacements,
                                        created_by, created_at, updated_by, updated_at )
        VALUES (
            #{downEventId}, ... ,
            #{assignedTechnician, typeHandler=io.nexcope.inform_note.base.util.json.JsonTypeHandler,
                                  javaType=io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician},
            #{approver, typeHandler=io.nexcope.inform_note.base.util.json.JsonTypeHandler,
                        javaType=io.nexcope.inform_note.domain.card.entity.vo.Approver},
            #{partReplacements, typeHandler=io.nexcope.inform_note.base.util.json.JsonListTypeHandler},
            #{createdBy}, SYSTIMESTAMP, #{updatedBy}, SYSTIMESTAMP
        )
    </insert>

    <update id="update" parameterType="io.nexcope.inform_note.domain.card.entity.DownEventCard">
        UPDATE tb_down_event_log
        SET work_status = #{workStatus},
            down_end_datetime = #{downEndDatetime},
            down_duration_minutes = #{downDurationMinutes},
            assigned_technician = #{assignedTechnician, typeHandler=io.nexcope.inform_note.base.util.json.JsonTypeHandler,
                                                        javaType=io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician},
            approver = #{approver, typeHandler=io.nexcope.inform_note.base.util.json.JsonTypeHandler,
                                   javaType=io.nexcope.inform_note.domain.card.entity.vo.Approver},
            part_replacements = #{partReplacements, typeHandler=io.nexcope.inform_note.base.util.json.JsonListTypeHandler},
            updated_by = #{updatedBy},
            updated_at = SYSTIMESTAMP
        WHERE down_event_id = #{downEventId}
    </update>

    <delete id="deleteById" parameterType="string">
        DELETE FROM tb_down_event_log WHERE down_event_id = #{downEventId}
    </delete>
</mapper>
```

**초안 대비 정정 포인트**

| 초안 | 실제 |
|---|---|
| `parameterType="com.informnote.domain.downevent.domain.DownEventLog"` | `io.nexcope.inform_note.domain.card.entity.DownEventCard` |
| `typeHandler=com.informnote.global.typehandler.JsonTypeHandler` | `io.nexcope.inform_note.base.util.json.JsonTypeHandler` |
| `PartReplacementListTypeHandler` (컬럼 전용 핸들러) | `JsonListTypeHandler` (범용) |
| `resultMap id="downEventLogResultMap"` | `downEventCardResultMap` |
| `#{criteria.offset}` / `#{criteria.equipmentId}` | `#{offset}` / `#{equipmentId}` (criteria 객체가 곧 parameterType) |
| `findAll` 만 존재 | `findByCriteria` + `countByCriteria` (+ `<sql id="searchConditions">`, `JSON_VALUE` 필터) |

---

## 6. 도메인 CRUD 계층: `*Logic` (실제 — `inform_note-domain`)

초안의 `DownEventLogService` 대신, **도메인 모듈 안의 `@Service` `*Logic`** 이 매퍼를 감쌉니다. (feature 모듈의 `*Flow`/`*Fetch` 가 이 `*Logic` 을 조합)

```java
package io.nexcope.inform_note.domain.card.logic;

@Service
@Transactional
@RequiredArgsConstructor
public class DownEventCardLogic {

    private final DownEventCardMapper mapper;

    public DownEventCard findById(String downEventId) {
        return mapper.findById(downEventId).orElseGet(null);   // ⚠️ 아래 NOTE
    }

    public OffsetElementList<DownEventCard> findOffsetElementListByCriteria(DownEventCardSearchCriteria criteria) {
        List<DownEventCard> results = mapper.findByCriteria(criteria);
        long totalCount           = mapper.countByCriteria(criteria);
        int offset = criteria != null ? criteria.getOffset() : 0;
        int limit  = criteria != null ? criteria.getLimit()  : 20;
        return OffsetElementList.of(results, totalCount, offset, limit);
    }

    public String registerDownEventCard(DownEventCardDto dto) {
        DownEventCard card = new DownEventCard(dto);
        mapper.insert(card);
        return card.getDownEventId();
    }

    public List<String> registerDownEventCards(List<DownEventCardDto> dtos) {
        return dtos.stream().map(dto -> {
            DownEventCard domain = new DownEventCard(dto);
            mapper.insert(domain);
            return domain.getDownEventId();
        }).toList();
    }

    public void modify(DownEventCard domain) { mapper.update(domain); }
    public int  deleteById(String downEventId) { return mapper.deleteById(downEventId); }
}
```

> [!NOTE]
> `findById` 의 `orElseGet(null)` 은 `Supplier` 자리에 `null` 을 넘기는 형태로, 결과가 없을 때 `NullPointerException` 이 발생합니다. **`orElse(null)` 로 수정** 필요(같은 모듈의 `DownContentLogic.findById` 는 `orElse(null)` 로 올바르게 구현되어 있음).

### 6.1. feature 계층 사용 예 (`MaintenanceFlow.correctiveAction`)

```java
DownEventCard card = downEventCardLogic.findById(downEventId);
if (card == null) throw new NoSuchElementException(downEventId + " 가 존재하지 않습니다.");
String empNo = newTechnician.getEmpNo();

if (WorkStatus.ACTION_DONE.equals(workStatus))
    card.completeAction(OffsetDateTime.now().toInstant().toEpochMilli(), empNo);
else
    card.changeWorkStatus(workStatus, empNo);      // IN_PROGRESS / VERIFIED / CLOSED

card.assignTechnician(newTechnician, empNo);       // 변경 시
card.specifyReplacement(newParts, empNo);          // 변경 시
downEventCardLogic.modify(card);                   // → mapper.update

// 본문 저장 + HTML 내 fileId 파싱하여 AttachedFile 확정
downContentLogic ... ;
attachedFileLogic.saveFiles(FileHandlerUtils.extractFileKeysFromHtml(contentHtml), downEventId, empNo);
```

---

## 7. `DownContent` (현상·조치 상세 본문, CLOB) 연동

### 7.1. `DownContent.java` (실제)

```java
package io.nexcope.inform_note.domain.content.entity;

@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class DownContent {
    private String downEventId;
    private String contentHtml;     // Froala HTML 본문 (Oracle CLOB)
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public DownContent(DownContentDto dto) {
        BeanUtils.copyProperties(dto, this);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static DownContent fromJson(String json) { return JsonUtil.fromJson(json, DownContent.class); }

    // 본문 수정 (초안의 modifyContent 아님)
    public void fromNewContent(String newHtml, String modifierId) {
        this.contentHtml = (newHtml != null) ? newHtml : "";
        this.updatedBy = modifierId;
    }
    public void fromNewContent(String newHtml) {
        this.contentHtml = (newHtml != null) ? newHtml : "";
    }
    public boolean hasContent() {
        return this.contentHtml != null && !this.contentHtml.trim().isEmpty();
    }
}
```

### 7.2. `DownContentMapper.java` (실제)

```java
package io.nexcope.inform_note.domain.content.mapper;

@Mapper
public interface DownContentMapper {
    Optional<DownContent> findById(@Param("downEventId") String downEventId);
    List<DownContent> findByIds(@Param("downEventIds") List<String> downEventIds);   // IN 조건 (뷰포트 다건 로딩)
    int insert(DownContent downContent);
    int update(DownContent downContent);
    int saveOrUpdate(DownContent downContent);   // Oracle MERGE
    int deleteById(@Param("downEventId") String downEventId);
}
```

### 7.3. `DownContentMapper.xml` (실제)

`<resultMap>` 없이 **`resultType` 단독** — CLOB `content_html` → `String` 자동 매핑. 모든 네임스페이스/타입은 `io.nexcope.inform_note.domain.content.*` 로 **일관** 되게 사용합니다(초안은 `com.informnote...` 와 혼용되어 있었음).

```xml
<mapper namespace="io.nexcope.inform_note.domain.content.mapper.DownContentMapper">

    <select id="findById" parameterType="string"
            resultType="io.nexcope.inform_note.domain.content.entity.DownContent">
        SELECT down_event_id, content_html, created_by, created_at, updated_by, updated_at
        FROM tb_down_content
        WHERE down_event_id = #{downEventId}
    </select>

    <select id="findByIds" resultType="io.nexcope.inform_note.domain.content.entity.DownContent">
        SELECT down_event_id, content_html, created_by, created_at, updated_by, updated_at
        FROM tb_down_content
        WHERE down_event_id IN
        <foreach item="id" collection="downEventIds" open="(" separator="," close=")">#{id}</foreach>
    </select>

    <insert id="insert" parameterType="io.nexcope.inform_note.domain.content.entity.DownContent">
        INSERT INTO tb_down_content (down_event_id, content_html, created_by, created_at, updated_by, updated_at)
        VALUES (#{downEventId}, #{contentHtml, jdbcType=CLOB}, #{createdBy}, SYSTIMESTAMP, #{updatedBy}, SYSTIMESTAMP)
    </insert>

    <update id="update" parameterType="io.nexcope.inform_note.domain.content.entity.DownContent">
        UPDATE tb_down_content
        SET content_html = #{contentHtml, jdbcType=CLOB}, updated_by = #{updatedBy}, updated_at = SYSTIMESTAMP
        WHERE down_event_id = #{downEventId}
    </update>

    <insert id="saveOrUpdate" parameterType="io.nexcope.inform_note.domain.content.entity.DownContent">
        MERGE INTO tb_down_content target
        USING (SELECT #{downEventId} AS down_event_id FROM DUAL) src
           ON (target.down_event_id = src.down_event_id)
        WHEN MATCHED THEN
            UPDATE SET target.content_html = #{contentHtml, jdbcType=CLOB},
                       target.updated_by   = #{updatedBy},
                       target.updated_at   = SYSTIMESTAMP
        WHEN NOT MATCHED THEN
            INSERT (down_event_id, content_html, created_by, created_at, updated_by, updated_at)
            VALUES (#{downEventId}, #{contentHtml, jdbcType=CLOB}, #{createdBy}, SYSTIMESTAMP, #{updatedBy}, SYSTIMESTAMP)
    </insert>

    <delete id="deleteById" parameterType="string">
        DELETE FROM tb_down_content WHERE down_event_id = #{downEventId}
    </delete>
</mapper>
```

---

## 8. DDL 대응 (요약 — `doc/schema_DDL.sql`)

| 테이블 | 컬럼 | 타입 | 엔티티 필드 |
|---|---|---|---|
| `tb_down_event_log` | `down_start_datetime` 등 | `NUMBER(19)` | `Long` (Epoch ms) |
| | `is_critical` | `BOOLEAN DEFAULT FALSE` (Oracle Native) | `boolean isCritical` |
| | `assigned_technician`, `approver`, `part_replacements` | `JSON` | `AssignedTechnician`, `Approver`, `List<PartReplacement>` |
| | `created_at`, `updated_at` | `TIMESTAMP WITH TIME ZONE` | `OffsetDateTime` |
| `tb_down_content` | `content_html` | `CLOB` (SECUREFILE) | `String` |
| | `down_event_id` | `VARCHAR2` + FK→`tb_down_event_log` `ON DELETE CASCADE` | `String` |

- JSON 값 검색은 `JSON_VALUE(...)` (예: `$.shift`, `$.empNo`), 배열 검색은 `MULTIVALUE INDEX` / `JSON_SEARCH` / `SEARCH INDEX ... FOR JSON` 를 활용합니다(DDL 참고).

---

## 9. 핵심 원칙 요약

1. **패키지·클래스명 현행화**: `io.nexcope.inform_note.domain.card.entity.DownEventCard` (테이블만 `tb_down_event_log`).
2. **최소 `<resultMap autoMapping="true">`**: 일반 컬럼은 카멜케이스 자동, JSON 3개만 TypeHandler 명시(+단일 VO 는 `javaType`).
3. **범용 TypeHandler 2개** (`JsonTypeHandler` / `JsonListTypeHandler`) + `AutoEnumTypeHandler` 전역 지정 — 컬럼별 핸들러 미생성.
4. **Rich Domain**: `DownEventCard` / `DownContent` 는 비즈니스 메서드를 갖고, `*Logic` → `*Flow`/`*Fetch` 에서 그대로 사용.
5. **페이징**: `findByCriteria` + `countByCriteria` → `OffsetElementList.of(results, totalCount, offset, limit)`.
6. **알려진 수정 필요**: `DownEventCardLogic.findById` 의 `orElseGet(null)` → `orElse(null)`, `JsonListTypeHandler` 후보 클래스명(`domain.log.entity.DownEventLog`) 정정.
