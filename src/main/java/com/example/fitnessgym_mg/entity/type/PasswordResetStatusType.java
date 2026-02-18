package com.example.fitnessgym_mg.entity.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import com.example.fitnessgym_mg.entity.enums.PasswordResetStatus;

/**
 * PostgreSQL ENUM型（password_reset_status）をPasswordResetStatus Enumにマッピングするカスタム型
 * <p>code値（"pending", "approved", "rejected"）を使用してPostgreSQL ENUM型とマッピングします。</p>
 */
public class PasswordResetStatusType implements JdbcType {

	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER; // PostgreSQL ENUM型はOTHER型として扱う
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<X>(javaType, this) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				if (value == null) {
					st.setNull(index, Types.OTHER);
				} else {
					PasswordResetStatus status = (PasswordResetStatus) value;
					// code値を取得してPostgreSQL ENUM型にキャスト
					// PostgreSQL JDBCドライバーは、Types.OTHERを使用してENUM型をマッピングします
					// setObjectを使用すると、PostgreSQL JDBCドライバーが自動的にENUM型にキャストしてくれます
					st.setObject(index, status.getCode(), Types.OTHER);
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				if (value == null) {
					st.setNull(name, Types.OTHER);
				} else {
					PasswordResetStatus status = (PasswordResetStatus) value;
					// code値を取得してPostgreSQL ENUM型にキャスト
					st.setObject(name, status.getCode(), Types.OTHER);
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<X>(javaType, this) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				String value = rs.getString(paramIndex);
				return value == null ? null : (X) PasswordResetStatus.fromCode(value);
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				String value = statement.getString(index);
				return value == null ? null : (X) PasswordResetStatus.fromCode(value);
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				String value = statement.getString(name);
				return value == null ? null : (X) PasswordResetStatus.fromCode(value);
			}
		};
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.OTHER;
	}
}
