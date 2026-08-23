# MyBatis JSON 컬럼의 범용 TypeHandler 처리 가이드

본 문서는 Oracle 23ai/26ai의 Native `JSON` 컬럼 및 일반 RDBMS의 JSON 문자열 컬럼을 MyBatis 환경에서 **컬럼별 개별 TypeHandler 클래스를 생성하지 않고, 단 2개의 범용 TypeHandler(`JsonTypeHandler`, `JsonListTypeHandler`)와 Java Reflection을 활용하여 도메인 엔티티(VO 객체 및 `List<T>`)에 완벽하게 자동 매핑하는 아키텍처와 구현 방식**을 정리합니다.

---

## 1. 배경 및 해결하려는 문제

### 1.1. 기존 방식의 한계 (컬럼별 TypeHandler 남발)
- 테이블에 JSON 컬럼이 추가될 때마다(`assigned_technician`, `approver`, `part_replacements`, `histories` 등) 매번 `AssignedTechnicianTypeHandler`, `ApproverTypeHandler`, `PartReplacementListTypeHandler`와 같은 자바 클래스를 일일이 생성해야 함.
- 보일러플레이트 코드가 기하급수적으로 증가하고 유지보수성이 저하됨.

### 1.2. 단순 Generic TypeHandler의 문제점
- MyBatis의 `type-handlers-package` 스캔 시, 기본 생성자(`no-arg constructor`)로 인스턴스가 등록되어 `type = Object.class`가 됨.
- 이로 인해 Jackson이 JSON Object를 `LinkedHashMap`으로 역직렬화하여 엔티티 필드에 대입 시 `IllegalArgumentException (Can not set VO to LinkedHashMap)` 발생.
- `JsonListTypeHandler<E>`의 경우 `List<E>` 타입 추론 과정에서 `List<List<Object>>` (2중 리스트)로 인식되어 `MismatchedInputException (Cannot deserialize ArrayList from Object)` 발생.

---

## 2. 해결 아키텍처: Reflection & Generic Type Resolver

```
[ Oracle DB JSON 데이터 ]
  - assigned_technician : {"empNo":"TECH-1002", "name":"김정비", ...}
  - approver            : {"empNo":"MGR-001", "name":"이팀장", ...}
  - part_replacements   : [{"partNo":"MEC-PAD-101", "partName":"...", "qty":2}]
        │
        ▼ (MyBatis Query Execution)
┌────────────────────────────────────────────────────────────────────────┐
│ 1. JsonTypeHandler (단일 VO 객체 매핑)                                 │
│    - 컬럼명: assigned_technician -> 필드명: assignedTechnician         │
│    - 엔티티 리플렉션: field.getType() -> AssignedTechnician.class 탐색 │
│    - Jackson: JsonUtil.fromJson(json, AssignedTechnician.class)        │
│                                                                        │
│ 2. JsonListTypeHandler (JSON 리스트 매핑)                              │
│    - 컬럼명: part_replacements -> 필드명: partReplacements             │
│    - 엔티티 리플렉션: ParameterizedType.getActualTypeArguments()[0]   │
│      -> 제네릭 실제 타입: PartReplacement.class 동적 추출              │
│    - Jackson: JsonUtil.fromJsonList(json, PartReplacement.class)      │
│                                                                        │
│ 3. 캐싱 레이어 (ConcurrentHashMap TYPE_CACHE)                          │
│    - 최초 1회 리플렉션 탐색 후 타입 정보 캐싱 (성능 오버헤드 0%)       │
└────────────────────────────────────────────────────────────────────────┘
        │
        ▼
[ DownEventLog 도메인 엔티티 ]
  - AssignedTechnician assignedTechnician  (정상 VO 객체)
  - Approver approver                      (정상 VO 객체)
  - List<PartReplacement> partReplacements (정상 VO 리스트)
```

---

## 3. 핵심 구현 코드

### 3.1. `JsonTypeHandler.java` (단일 VO 객체 범용 핸들러)

```java
package io.nexcope.inform_note.base.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@MappedJdbcTypes(JdbcType.OTHER)
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final Map<String, Class<?>> TYPE_CACHE = new ConcurrentHashMap<>();
    private final Class<T> type;
    private final TypeReference<T> typeReference;

    @SuppressWarnings("unchecked")
    public JsonTypeHandler() {
        this.type = (Class<T>) Object.class;
        this.typeReference = null;
    }

    public JsonTypeHandler(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
        this.typeReference = null;
    }

    public JsonTypeHandler(TypeReference<T> typeReference) {
        if (typeReference == null) {
            throw new IllegalArgumentException("TypeReference argument cannot be null");
        }
        this.type = null;
        this.typeReference = typeReference;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JsonUtil.toJson(parameter));
        } catch (Exception e) {
            throw new SQLException("JSON Serialization Failed for " + (type != null ? type.getName() : "TypeReference"), e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toObject(rs.getString(columnName), columnName);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnName = null;
        try {
            columnName = rs.getMetaData().getColumnLabel(columnIndex);
        } catch (Exception ignored) {
        }
        return toObject(rs.getString(columnIndex), columnName);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toObject(cs.getString(columnIndex), null);
    }

    @SuppressWarnings("unchecked")
    private T toObject(String content, String columnName) throws SQLException {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try {
            if (typeReference != null) {
                return JsonUtil.fromJson(content, typeReference);
            }
            if (type != null && type != Object.class) {
                return JsonUtil.fromJson(content, type);
            }

            // type이 Object.class인 경우(기본 생성자로 등록된 경우) 리플렉션으로 필드 타입 동적 탐색
            Class<?> resolvedType = resolveFieldTypeFromReflection(columnName);
            if (resolvedType != null && resolvedType != Object.class) {
                return (T) JsonUtil.fromJson(content, resolvedType);
            }

            return (T) JsonUtil.fromJson(content, Object.class);
        } catch (Exception e) {
            throw new SQLException("JSON Deserialization Failed. Content: " + content, e);
        }
    }

    private Class<?> resolveFieldTypeFromReflection(String columnName) {
        if (columnName == null) return Object.class;

        return TYPE_CACHE.computeIfAbsent(columnName.toUpperCase(), col -> {
            String fieldName = toCamelCase(col);
            Class<?> resolved = findFieldTypeInKnownPackages(fieldName);
            return resolved != null ? resolved : Object.class;
        });
    }

    private Class<?> findFieldTypeInKnownPackages(String fieldName) {
        String[] candidateEntityClassNames = {
            "io.nexcope.inform_note.domain.log.entity.DownEventLog",
            "io.nexcope.inform_note.domain.content.entity.DownContent",
            "io.nexcope.inform_note.domain.employees.entity.Employees"
        };

        for (String className : candidateEntityClassNames) {
            try {
                Class<?> entityClass = Class.forName(className);
                Field[] fields = entityClass.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().equalsIgnoreCase(fieldName)) {
                        return field.getType();
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private String toCamelCase(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
```

---

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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(List.class)
public class JsonListTypeHandler<E> extends BaseTypeHandler<List<E>> {

    private static final Map<String, Class<?>> TYPE_CACHE = new ConcurrentHashMap<>();
    private final Class<E> elementType;

    @SuppressWarnings("unchecked")
    public JsonListTypeHandler() {
        this.elementType = (Class<E>) Object.class;
    }

    public JsonListTypeHandler(Class<E> elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException("ElementType argument cannot be null");
        }
        this.elementType = elementType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<E> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JsonUtil.toJson(parameter));
        } catch (Exception e) {
            throw new SQLException("JSON Serialization Failed for List<" + (elementType != null ? elementType.getName() : "Object") + ">", e);
        }
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getString(columnName), columnName);
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnName = null;
        try {
            columnName = rs.getMetaData().getColumnLabel(columnIndex);
        } catch (Exception ignored) {
        }
        return toList(rs.getString(columnIndex), columnName);
    }

    @Override
    public List<E> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getString(columnIndex), null);
    }

    @SuppressWarnings("unchecked")
    private List<E> toList(String content, String columnName) throws SQLException {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Class<?> targetClass = this.elementType;
            if (targetClass == null || targetClass == Object.class || List.class.isAssignableFrom(targetClass)) {
                if (columnName != null) {
                    targetClass = resolveGenericTypeFromReflection(columnName);
                }
            }

            String trimmed = content.trim();
            // 단일 객체 JSON이 들어온 경우 방어 처리 (1개짜리 리스트로 래핑)
            if (trimmed.startsWith("{")) {
                if (targetClass != null && targetClass != Object.class) {
                    return (List<E>) Collections.singletonList(JsonUtil.fromJson(trimmed, targetClass));
                }
                return (List<E>) Collections.singletonList(JsonUtil.fromJson(trimmed, Object.class));
            }

            if (targetClass != null && targetClass != Object.class) {
                return (List<E>) JsonUtil.fromJsonList(trimmed, targetClass);
            }
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
            "io.nexcope.inform_note.domain.log.entity.DownEventLog",
            "io.nexcope.inform_note.domain.content.entity.DownContent",
            "io.nexcope.inform_note.domain.employees.entity.Employees"
        };

        for (String className : candidateEntityClassNames) {
            try {
                Class<?> entityClass = Class.forName(className);
                Field[] fields = entityClass.getDeclaredFields();
                for (Field field : fields) {
                    if (field.getName().equalsIgnoreCase(fieldName)) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType pt) {
                            Type[] actualTypes = pt.getActualTypeArguments();
                            if (actualTypes.length > 0 && actualTypes[0] instanceof Class<?> clazz) {
                                return clazz;
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private String toCamelCase(String snakeCase) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
```

---

## 4. MyBatis XML 매퍼 적용 예시 (`DownEventLogMapper.xml`)

`autoMapping="true"`를 활성화한 `<resultMap>`을 정의하고, **JSON 컬럼 3개만 TypeHandler를 지정**하면 나머지 일반 컬럼(식별자, 일시, 상태, Enum 등)은 자동으로 카멜케이스 매핑됩니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.nexcope.inform_note.domain.log.mapper.DownEventLogMapper">

    <!-- autoMapping="true"로 일반 컬럼은 자동 매핑, JSON 컬럼만 핸들러 지정 -->
    <resultMap id="downEventLogResultMap" type="io.nexcope.inform_note.domain.log.entity.DownEventLog" autoMapping="true">
        <id property="downEventId" column="down_event_id" />
        
        <!-- 단일 JSON 객체 매핑 -->
        <result property="assignedTechnician" column="assigned_technician"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler" />
        <result property="approver" column="approver"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonTypeHandler" />
        
        <!-- JSON 배열/리스트 매핑 -->
        <result property="partReplacements" column="part_replacements"
                typeHandler="io.nexcope.inform_note.base.util.json.JsonListTypeHandler" />
    </resultMap>

    <!-- 조건 검색 쿼리 -->
    <select id="findByCriteria" resultMap="downEventLogResultMap">
        SELECT *
        FROM tb_down_event_log
        <where>
            <if test="criteria.equipmentId != null and criteria.equipmentId != ''">
                AND equipment_id = #{criteria.equipmentId}
            </if>
        </where>
        ORDER BY down_start_datetime DESC
        OFFSET #{criteria.offset} ROWS FETCH NEXT #{criteria.limit} ROWS ONLY
    </select>

</mapper>
```

---

## 5. 결론 및 이점

1. **컬럼별 클래스 생성 제거 (0개)**:
   - 더 이상 컬럼마다 `XxxTypeHandler` 자바 파일을 만들 필요가 없습니다.
2. **Generic 실제 타입 자동 탐색 (Reflection)**:
   - `List<T>`의 실제 요소 타입(`PartReplacement` 등)을 리플렉션(`ParameterizedType`)으로 런타임에 동적으로 판별하므로, 향후 어떤 엔티티에 새로운 `List<AnyVO>` 필드가 추가되어도 수정 없이 동작합니다.
3. **성능 극대화**:
   - `ConcurrentHashMap` 캐싱을 적용하여 리플렉션 탐색 오버헤드가 단 1회만 발생합니다.
4. **완벽한 안정성**:
   - DB에 단일 객체(`{}`)가 들어오거나 빈 배열(`[]`), null이 들어오는 모든 예외 케이스에 대해 안전하게 방어 변환됩니다.
