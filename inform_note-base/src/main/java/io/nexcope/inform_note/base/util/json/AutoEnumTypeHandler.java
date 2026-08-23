package io.nexcope.inform_note.base.util.json;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AutoEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private Class<E> type;

    public AutoEnumTypeHandler() {
    }

    public AutoEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convert(rs.getString(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convert(rs.getString(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convert(cs.getString(columnIndex));
    }

    private E convert(String s) {
        if (s == null || s.trim().isEmpty() || type == null) {
            return null;
        }

        String normalized = s.trim().toUpperCase();
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(normalized)) {
                return e;
            }
        }
        // 하이픈(-), 콜론(:), 공백, 슬래시(/) 등을 언더스코어(_)로 변환하여 재시도 (예: FAB-2 -> FAB_2)
        String replaced = normalized.replaceAll("[-: /]", "_");
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(replaced)) {
                return e;
            }
        }
        return null;
    }
}
