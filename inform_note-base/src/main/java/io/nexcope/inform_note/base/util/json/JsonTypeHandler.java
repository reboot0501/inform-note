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

    // [전역 캐시] 컬럼명(SNAKE_CASE) -> 대상 VO/필드 Class
    private static final Map<String, Class<?>> FIELD_TYPE_MAP = new ConcurrentHashMap<>();
    private static volatile boolean isScanned = false;

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

            // 1. type이 Object인 경우 자동 스캔된 리플렉션 맵에서 타입 추출
            Class<?> resolvedType = resolveFieldType(columnName);
            if (resolvedType != null && resolvedType != Object.class) {
                return (T) JsonUtil.fromJson(content, resolvedType);
            }

            return (T) JsonUtil.fromJson(content, Object.class);
        } catch (Exception e) {
            throw new SQLException("JSON Deserialization Failed for " + (type != null ? type.getName() : "TypeReference") + " Content: " + content, e);
        }
    }

    /**
     * 컬럼명에 매칭되는 필드 타입을 도메인 패키지 자동 스캔을 통해 결정
     */
    private Class<?> resolveFieldType(String columnName) {
        if (columnName == null) return Object.class;
        ensureDomainClassesScanned();
        return FIELD_TYPE_MAP.getOrDefault(columnName.toUpperCase(), Object.class);
    }

    /**
     * io.nexcope.inform_note.domain 하위의 모든 Entity 클래스를 자동 스캔하여 필드 매핑 등록
     */
    private synchronized void ensureDomainClassesScanned() {
        if (isScanned) return;

        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            // entity 패키지 하위 클래스 대상 필터
            scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile("io\\.nexcope\\.inform_note\\.domain\\..*\\.entity\\..*")));

            Set<BeanDefinition> candidates = scanner.findCandidateComponents("io.nexcope.inform_note.domain");
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

            for (BeanDefinition beanDef : candidates) {
                try {
                    Class<?> clazz = ClassUtils.forName(beanDef.getBeanClassName(), classLoader);
                    for (Field field : clazz.getDeclaredFields()) {
                        String snakeName = toSnakeCase(field.getName()).toUpperCase();
                        // Primitive/String/표준 타입을 제외한 VO 객체만 캐싱
                        if (!field.getType().isPrimitive() 
                                && !field.getType().getName().startsWith("java.lang") 
                                && !field.getType().getName().startsWith("java.time")) {
                            FIELD_TYPE_MAP.putIfAbsent(snakeName, field.getType());
                        }
                    }
                } catch (Throwable ignored) {
                }
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
