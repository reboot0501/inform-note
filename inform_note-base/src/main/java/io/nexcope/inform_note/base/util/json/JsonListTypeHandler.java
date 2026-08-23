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
        return toList(rs.getString(columnName), columnName, rs);
    }

    @Override
    public List<E> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnName = null;
        try {
            columnName = rs.getMetaData().getColumnLabel(columnIndex);
        } catch (Exception ignored) {
        }
        return toList(rs.getString(columnIndex), columnName, rs);
    }

    @Override
    public List<E> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getString(columnIndex), null, null);
    }

    @SuppressWarnings("unchecked")
    private List<E> toList(String content, String columnName, ResultSet rs) throws SQLException {
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
            
            // 클래스로더를 통해 도메인 엔티티를 리플렉션 탐색
            Class<?> resolved = findGenericTypeInKnownPackages(fieldName);
            return resolved != null ? resolved : Object.class;
        });
    }

    private Class<?> findGenericTypeInKnownPackages(String fieldName) {
        // 주요 도메인 엔티티 클래스 후보 목록
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
