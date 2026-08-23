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

            // type이 Object.class인 경우 리플렉션으로 엔티티의 필드 타입을 동적 탐색
            Class<?> resolvedType = resolveFieldTypeFromReflection(columnName);
            if (resolvedType != null && resolvedType != Object.class) {
                return (T) JsonUtil.fromJson(content, resolvedType);
            }

            return (T) JsonUtil.fromJson(content, Object.class);
        } catch (Exception e) {
            throw new SQLException("JSON Deserialization Failed for " + (type != null ? type.getName() : "TypeReference") + " Content: " + content, e);
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
