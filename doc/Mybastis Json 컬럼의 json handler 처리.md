# MyBatis JSON 컬럼의 범용 TypeHandler 처리 가이드

Oracle 23ai/26ai의 Native `JSON` 컬럼(및 일반 RDBMS의 JSON 문자열 컬럼)을 MyBatis 환경에서 **컬럼별 TypeHandler 클래스를 만들지 않고, 범용 TypeHandler 2개(`JsonTypeHandler`, `JsonListTypeHandler`) + 리플렉션으로 도메인 엔티티(VO 및 `List<T>`)에 자동 매핑**하는 구현 방식을 정리합니다.

- 위치: `inform_note-base` 모듈 `io.nexcope.inform_note.base.util.json`
- 적용 대상 컬럼: `tb_down_event_log.assigned_technician`(`AssignedTechnician`), `approver`(`Approver`), `part_replacements`(`List<PartReplacement>`)
- 관련 유틸: `JsonUtil`(Jackson `ObjectMapper` 래퍼), `AutoEnumTypeHandler`(Enum 유연 매핑)

---

## 1. 배경 및 해결하려는 문제

### 1.1. 기존 방식의 한계 (컬럼별 TypeHandler 남발)
- JSON 컬럼이 추가될 때마다 `AssignedTechnicianTypeHandler`, `ApproverTypeHandler`, `PartReplacementListTypeHandler` … 를 일일이 만들어야 함 → 보일러플레이트 폭증.

### 1.2. 단순 Generic TypeHandler 의 문제점
- `type-handlers-package` 스캔 시 **기본 생성자(no-arg)** 로 인스턴스가 등록되어 `type = Object.class` 가 됨.
- Jackson 이 JSON Object 를 `LinkedHashMap` 으로 역직렬화 → 엔티티 VO 필드 대입 시 타입 불일치.
- `List<E>` 는 요소 타입(`E`) 정보가 소거되어 `List<Object>`(사실상 `List<LinkedHashMap>`)로 역직렬화됨.

### 1.3. 본 프로젝트의 해결 방향
- **`JsonTypeHandler`**: no-arg 로 등록되면 `io.nexcope.inform_note.domain..*.entity..*` 를 **classpath 자동 스캔**하여 `컬럼명(SNAKE) → 필드 타입 Class` 맵(`FIELD_TYPE_MAP`)을 1회 구축하고, 조회 시 컬럼명으로 대상 VO 타입을 찾아 역직렬화.
- **`JsonListTypeHandler`**: 컬럼명 → 필드명(camelCase) → 리플렉션으로 `ParameterizedType.getActualTypeArguments()[0]`(예: `PartReplacement.class`)을 추출하여 `List<T>` 요소 타입 결정.
- 두 핸들러 모두 `ConcurrentHashMap` 캐싱으로 리플렉션 오버헤드를 최초 1회로 제한.

---

## 2. MyBatis 등록 방식 (`application.yaml`)

```yaml
mybatis:
  type-aliases-package: io.nexcope.inform_note.domain
  type-handlers-package: io.nexcope.inform_note.base.util.json   # ← 이 패키지의 TypeHandler 자동 등록
  configuration:
    map-underscore-to-camel-case: true
    call-setters-on-nulls: true
    jdbc-type-for-null: varchar
    default-enum-type-handler: io.nexcope.inform_note.base.util.json.AutoEnumTypeHandler
    auto-mapping-unknown-column-behavior: none
```

- `type-handlers-package` 스캔으로 `JsonTypeHandler`, `JsonListTypeHandler`, `AutoEnumTypeHandler` 가 **no-arg 생성자** 로 전역 등록됩니다.
- 이 때문에 각 핸들러는 `type`/`elementType` 을 알 수 없는 상태(`Object.class`)로 시작하며, 아래 3장의 **리플렉션 타입 해석 로직** 이 필요합니다.
- `default-enum-type-handler` 로 모든 Enum 컬럼은 `AutoEnumTypeHandler` 가 처리합니다(`FAB-1` → `FAB_1` 처럼 하이픈/공백/슬래시를 언더스코어로 치환 후 재매칭).

---

## 3. 핵심 구현 코드 (실제 소스)

### 3.1. `JsonTypeHandler.java` (단일 VO 객체 범용 핸들러 — classpath 자동 스캔)

```java
package io.nexcope.inform_note.base.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@MappedJdbcTypes(JdbcType.OTHER)
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    // [전역 캐시] 컬럼명(SNAKE_CASE 대문자) -> 대상 VO/필드 Class
    private static final Map<String, Class<?>> FIELD_TYPE_MAP = new ConcurrentHashMap<>();
    private static volatile boolean isScanned = false;

    private final Class<T> type;
    private final TypeReference<T> typeReference;

    @SuppressWarnings("unchecked")
    public JsonTypeHandler() {                 // type-handlers-package 스캔 시 사용되는 no-arg 생성자
        this.type = (Class<T>) Object.class;
        this.typeReference = null;
    }

    public JsonTypeHandler(Class<T> type) {    // XML 에서 javaType 을 함께 지정하면 이 생성자 사용
        if (type == null) throw new IllegalArgumentException("Type argument cannot be null");
        this.type = type;
        this.typeReference = null;
    }

    public JsonTypeHandler(TypeReference<T> typeReference) {
        if (typeReference == null) throw new IllegalArgumentException("TypeReference argument cannot be null");
        this.type = null;
        this.typeReference = typeReference;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JsonUtil.toJson(parameter));   // 객체 -> JSON 문자열
        } catch (Exception e) {
            throw new SQLException("JSON Serialization Failed for "
                    + (type != null ? type.getName() : "TypeReference"), e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toObject(rs.getString(columnName), columnName);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnName = null;
        try { columnName = rs.getMetaData().getColumnLabel(columnIndex); } catch (Exception ignored) {}
        return toObject(rs.getString(columnIndex), columnName);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toObject(cs.getString(columnIndex), null);
    }

    @SuppressWarnings("unchecked")
    private T toObject(String content, String columnName) throws SQLException {
        if (content == null || content.trim().isEmpty()) return null;
        try {
            if (typeReference != null) return JsonUtil.fromJson(content, typeReference);
            if (type != null && type != Object.class) return JsonUtil.fromJson(content, type);

            // type 이 Object 인 경우(no-arg 등록) 자동 스캔된 맵에서 타입 추출
            Class<?> resolvedType = resolveFieldType(columnName);
            if (resolvedType != null && resolvedType != Object.class) {
                return (T) JsonUtil.fromJson(content, resolvedType);
            }
            return (T) JsonUtil.fromJson(content, Object.class);
        } catch (Exception e) {
            throw new SQLException("JSON Deserialization Failed for "
                    + (type != null ? type.getName() : "TypeReference") + " Content: " + content, e);
        }
    }

    private Class<?> resolveFieldType(String columnName) {
        if (columnName == null) return Object.class;
        ensureDomainClassesScanned();
        return FIELD_TYPE_MAP.getOrDefault(columnName.toUpperCase(), Object.class);
    }

    /** io.nexcope.inform_note.domain 하위 *.entity.* 클래스를 자동 스캔하여 필드 타입 맵 구축 */
    private synchronized void ensureDomainClassesScanned() {
        if (isScanned) return;
        try {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new RegexPatternTypeFilter(
                    Pattern.compile("io\\.nexcope\\.inform_note\\.domain\\..*\\.entity\\..*")));

            Set<BeanDefinition> candidates =
                    scanner.findCandidateComponents("io.nexcope.inform_note.domain");
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

            for (BeanDefinition beanDef : candidates) {
                try {
                    Class<?> clazz = ClassUtils.forName(beanDef.getBeanClassName(), classLoader);
                    for (Field field : clazz.getDeclaredFields()) {
                        String snakeName = toSnakeCase(field.getName()).toUpperCase();
                        // primitive / java.lang / java.time 을 제외한 VO 객체만 캐싱
                        if (!field.getType().isPrimitive()
                                && !field.getType().getName().startsWith("java.lang")
                                && !field.getType().getName().startsWith("java.time")) {
                            FIELD_TYPE_MAP.putIfAbsent(snakeName, field.getType());
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        } finally {
            isScanned = true;
        }
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
```

> [!IMPORTANT]
> - 문서 초안에 있던 `findFieldTypeInKnownPackages` (하드코딩된 3개 엔티티 후보 배열) 방식은 **더 이상 사용하지 않습니다.**
>   실제 코드는 `ClassPathScanningCandidateComponentProvider` 로 도메인 `entity` 패키지 전체를 스캔하므로, 새로운 엔티티에 새 VO 필드가 추가되어도 수정이 필요 없습니다.
> - 스캔은 `synchronized` + `volatile isScanned` 로 1회만 수행되며, 이후에는 `FIELD_TYPE_MAP` 조회만 발생합니다.
> - 서로 다른 엔티티에 **동일 필드명(→ 동일 컬럼명)** 이 서로 다른 타입으로 존재하면 `putIfAbsent` 특성상 **먼저 스캔된 타입이 우선**됩니다. 이런 경우에는 XML `<result>` 에 `javaType` 을 명시해 모호성을 제거하세요(3.3 참고).

### 3.2. `JsonListTypeHandler.java` (JSON 리스트 범용 핸들러)

```java
package io.nexcope.inform_note.base.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(List.class)
public class JsonListTypeHandler<E> extends BaseTypeHandler<List<E>> {

    private static final Map<String, Class<?>> TYPE_CACHE = new ConcurrentHashMap<>();
    private final Class<E> elementType;

    @SuppressWarnings("unchecked")
    public JsonListTypeHandler() { this.elementType = (Class<E>) Object.class; }

    public JsonListTypeHandler(Class<E> elementType) {
        if (elementType == null) throw new IllegalArgumentException("ElementType argument cannot be null");
        this.elementType = elementType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<E> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JsonUtil.toJson(parameter));
        } catch (Exception e) {
            throw new SQLException("JSON Serialization Failed for List<"
                    + (elementType != null ? elementType.getName() : "Object") + ">", e);
        }
    }

    @Override public List<E> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getString(columnName), columnName, rs);
    }
    @Override public List<E> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnName = null;
        try { columnName = rs.getMetaData().getColumnLabel(columnIndex); } catch (Exception ignored) {}
        return toList(rs.getString(columnIndex), columnName, rs);
    }
    @Override public List<E> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getString(columnIndex), null, null);
    }

    @SuppressWarnings("unchecked")
    private List<E> toList(String content, String columnName, ResultSet rs) throws SQLException {
        if (content == null || content.trim().isEmpty()) return new ArrayList<>();
        try {
            Class<?> targetClass = this.elementType;
            if (targetClass == null || targetClass == Object.class || List.class.isAssignableFrom(targetClass)) {
                if (columnName != null) targetClass = resolveGenericTypeFromReflection(columnName);
            }

            String trimmed = content.trim();
            // 단일 객체 JSON('{')이 들어온 경우 1개짜리 리스트로 방어 래핑
            if (trimmed.startsWith("{")) {
                if (targetClass != null && targetClass != Object.class)
                    return (List<E>) Collections.singletonList(JsonUtil.fromJson(trimmed, targetClass));
                return (List<E>) Collections.singletonList(JsonUtil.fromJson(trimmed, Object.class));
            }
            if (targetClass != null && targetClass != Object.class)
                return (List<E>) JsonUtil.fromJsonList(trimmed, targetClass);
            return (List<E>) JsonUtil.fromJson(trimmed, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            throw new SQLException("JSON Deserialization Failed for List Content: " + content, e);
        }
    }

    private Class<?> resolveGenericTypeFromReflection(String columnName) {
        if (columnName == null) return Object.class;
        return TYPE_CACHE.computeIfAbsent(columnName.toUpperCase(), col -> {
            String fieldName = toCamelCase(col);
            Class<?> resolved = findGenericTypeInKnownPackages(fieldName);
            return resolved != null ? resolved : Object.class;
        });
    }

    private Class<?> findGenericTypeInKnownPackages(String fieldName) {
        String[] candidateEntityClassNames = {
            "io.nexcope.inform_note.domain.log.entity.DownEventLog",   // ⚠️ 존재하지 않는 클래스 (아래 IMPORTANT 참고)
            "io.nexcope.inform_note.domain.content.entity.DownContent",
            "io.nexcope.inform_note.domain.employees.entity.Employees"
        };
        for (String className : candidateEntityClassNames) {
            try {
                Class<?> entityClass = Class.forName(className);
                for (Field field : entityClass.getDeclaredFields()) {
                    if (field.getName().equalsIgnoreCase(fieldName)) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType pt) {
                            Type[] actualTypes = pt.getActualTypeArguments();
                            if (actualTypes.length > 0 && actualTypes[0] instanceof Class<?> clazz) return clazz;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private String toCamelCase(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') nextUpper = true;
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}
```

> [!IMPORTANT]
> **현재 코드 이슈 — `JsonListTypeHandler` 의 하드코딩 후보 목록이 낡음**
> `findGenericTypeInKnownPackages` 의 후보 배열 첫 항목이 `io.nexcope.inform_note.domain.log.entity.DownEventLog` 인데, 실제 클래스는 **`io.nexcope.inform_note.domain.card.entity.DownEventCard`** 로 리네임/이동되었습니다.
> 따라서 `part_replacements` 컬럼을 컬럼명 기반으로 해석하면 후보 클래스에서 `partReplacements` 필드를 찾지 못해 요소 타입이 `Object.class` 로 폴백됩니다(→ `List<LinkedHashMap>`).
> **회피/수정 방안 (택1)**
> 1. (권장) `JsonTypeHandler` 와 동일하게 `ClassPathScanningCandidateComponentProvider` 로 도메인 `entity` 패키지를 자동 스캔하도록 변경.
> 2. 후보 배열을 `io.nexcope.inform_note.domain.card.entity.DownEventCard` 로 정정.
> 3. **XML `<result>` 에 요소 타입을 명시** — 아래 3.3 처럼 `javaType` 또는 `#{...}` 인라인에 `javaType` 을 지정하면 리플렉션 폴백 자체를 타지 않습니다.
> 현재 `DownEventCardMapper.xml` 은 `partReplacements` 에 `javaType` 을 지정하지 않고 있으므로, 부품 목록을 실제로 사용하는 시점에 위 이슈가 드러날 수 있습니다.

### 3.3. `AutoEnumTypeHandler.java` (Enum 유연 매핑 — 참고)

```java
// FAB-1, "PM MAINTENANCE" 같이 하이픈/공백/슬래시/콜론이 섞인 DB 값을 언더스코어로 치환 후 재매칭
private E convert(String s) {
    if (s == null || s.trim().isEmpty() || type == null) return null;
    String normalized = s.trim().toUpperCase();
    for (E e : type.getEnumConstants()) if (e.name().equalsIgnoreCase(normalized)) return e;
    String replaced = normalized.replaceAll("[-: /]", "_");
    for (E e : type.getEnumConstants()) if (e.name().equalsIgnoreCase(replaced)) return e;
    return null;
}
```

- `application.yaml` 의 `default-enum-type-handler` 로 전역 지정되어 있어, XML 에서 Enum 컬럼에 별도 `typeHandler` 를 붙일 필요가 없습니다.

---

## 4. MyBatis XML 매퍼 적용 (`DownEventCardMapper.xml` — 실제)

`autoMapping="true"` `<resultMap>` 에서 **일반 컬럼은 카멜케이스 자동 매핑**, **JSON 컬럼 3개만** TypeHandler 를 지정합니다.
단일 객체 컬럼(`assigned_technician`, `approver`)에는 `javaType` 을 함께 명시하여 `JsonTypeHandler(Class)` 생성자가 쓰이도록 합니다.

```xml
<mapper namespace="io.nexcope.inform_note.domain.card.mapper.DownEventCardMapper">

    <resultMap id="downEventCardResultMap"
               type="io.nexcope.inform_note.domain.card.entity.DownEventCard" autoMapping="true">
        <result property="assignedTechnician" column="assigned_technician"
                javaType="io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler"/>
        <result property="approver" column="approver"
                javaType="io.nexcope.inform_note.domain.card.entity.vo.Approver"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler"/>
        <!-- ⚠️ 요소 타입(javaType) 미지정 — 3.2 IMPORTANT 참고 -->
        <result property="partReplacements" column="part_replacements"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonListTypeHandler"/>
    </resultMap>

    <!-- 동적 검색 조건 (findByCriteria / countByCriteria 공용) -->
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
            <!-- JSON 컬럼 내부 값 필터 (Oracle JSON_VALUE) -->
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

    <select id="findByCriteria"
            parameterType="io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria"
            resultMap="downEventCardResultMap">
        SELECT /* 컬럼 명시 */ down_event_id, equipment_id, /* ... */ assigned_technician, approver, part_replacements,
               created_by, created_at, updated_by, updated_at
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
</mapper>
```

> [!NOTE]
> - `findByCriteria` 의 `parameterType` 은 `DownEventCardSearchCriteria` **객체 자체** 이므로, 바인딩 표현식은 `#{criteria.offset}` 이 아니라 **`#{offset}` / `#{fabricationPlant}`** 형태입니다. (초안 문서의 `#{criteria.xxx}` 표기는 오류)
> - `#{offset}` / `#{limit}` 은 `DownEventCardSearchCriteria` 의 `getOffset()` / `getLimit()` 게터(페이지·사이즈 기본 20 계산 포함)로 해석됩니다.
> - INSERT/UPDATE 인라인 바인딩에서도 단일 VO 는 `javaType` 을 함께 지정합니다:
>   `#{assignedTechnician, typeHandler=io.nexcope.inform_note.base.util.json.JsonTypeHandler, javaType=io.nexcope.inform_note.domain.card.entity.vo.AssignedTechnician}`

---

## 5. 동작 요약

```
[ Oracle JSON 컬럼 ]
  assigned_technician : {"empNo":"TECH-1002","name":"김정비","jobTitle":"...","shift":"A"}
  approver            : {"empNo":"MGR-001","JobTitle":"...","name":"이팀장","approvedAt":...}
  part_replacements   : [{"replacementType":"REPLACEMENT_PART","partNo":"...","partName":"...","qty":2}]
        │  (MyBatis <resultMap autoMapping="true">)
        ▼
  ┌ 일반 컬럼 ─ map-underscore-to-camel-case 로 자동 매핑
  ├ Enum 컬럼 ─ AutoEnumTypeHandler (전역 default-enum-type-handler)
  ├ assigned_technician / approver ─ JsonTypeHandler(+javaType) → JsonUtil.fromJson(json, VO.class)
  └ part_replacements ─ JsonListTypeHandler → 요소 타입 리플렉션 해석 → JsonUtil.fromJsonList(json, E.class)
        ▼
[ DownEventCard 엔티티 ]  AssignedTechnician / Approver / List<PartReplacement>
```

---

## 6. 이점과 주의점

| 이점 | 설명 |
|---|---|
| 컬럼별 핸들러 0개 | `JsonTypeHandler` / `JsonListTypeHandler` 2개로 모든 JSON 컬럼 처리 |
| 자동 타입 해석 | `JsonTypeHandler` 는 도메인 `entity` 패키지 classpath 자동 스캔으로 컬럼→VO 타입 매핑 |
| 성능 | `ConcurrentHashMap` + `volatile` 캐시로 리플렉션/스캔 1회 |
| 방어 변환 | `null`, 빈 문자열, 단일 객체(`{}`)가 리스트 컬럼에 들어와도 안전 처리 |

| 주의점 | 대응 |
|---|---|
| `JsonListTypeHandler` 후보 클래스명이 낡음 (`domain.log.entity.DownEventLog`) | 자동 스캔 방식으로 통일하거나 `DownEventCard` 로 정정, 또는 XML 에 요소 `javaType` 명시 (3.2 IMPORTANT) |
| 서로 다른 엔티티의 동일 필드명 충돌 | `FIELD_TYPE_MAP` 은 `putIfAbsent` (선스캔 우선) → XML `<result javaType=...>` 로 확정 |
| `Approver.JobTitle` 필드가 대문자 시작 | Jackson 기본 매핑상 JSON 키도 `JobTitle` 로 직렬화됨에 유의 (프론트 계약 확인) |
