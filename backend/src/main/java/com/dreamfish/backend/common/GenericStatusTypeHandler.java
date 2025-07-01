package com.dreamfish.backend.common;

import com.dreamfish.backend.entity.status.BaseStatus;
import com.dreamfish.backend.exception.BaseException;
import lombok.Getter;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Dream fish
 * @version 1.0
 * @description: MyBatis枚举类型处理器:对BaseStatus子类进行处理，实现按状态码映射
 * @date 2025/4/10 16:13
 */
@Getter
public class GenericStatusTypeHandler<E extends Enum<E> & BaseStatus> extends BaseTypeHandler<E> {
    private final Class<E> type;

    public GenericStatusTypeHandler(Class<E> type) {
        if (type == null) {
            throw new BaseException(ErrorCode.TYPE_NOT_MATCH, "Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, E parameter, JdbcType jdbcType
    ) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : BaseStatus.fromCode(type, code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : BaseStatus.fromCode(type, code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : BaseStatus.fromCode(type, code);
    }
}