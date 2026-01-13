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

import com.example.fitnessgym_mg.entity.enums.Gender;

/**
 * PostgreSQL ENUM型（customer_gender）をGender Enumにマッピングするカスタム型
 * <p>日本語ラベル（「男」「女」）を使用してPostgreSQL ENUM型とマッピングします。</p>
 */
public class GenderType implements JdbcType {

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
					Gender gender = (Gender) value;
					// 日本語ラベル（「男」「女」）を取得してPostgreSQL ENUM型にキャスト
					// PostgreSQL JDBCドライバーは、Types.OTHERを使用してENUM型をマッピングします
					// setObjectを使用すると、PostgreSQL JDBCドライバーが自動的にENUM型にキャストしてくれます
					st.setObject(index, gender.getLabel(), Types.OTHER);
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				if (value == null) {
					st.setNull(name, Types.OTHER);
				} else {
					Gender gender = (Gender) value;
					// 日本語ラベル（「男」「女」）を取得してPostgreSQL ENUM型にキャスト
					st.setObject(name, gender.getLabel(), Types.OTHER);
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
					if (value == null) {
						return null;
					}
					// まずENUM名として試行
					try {
						return (X) Gender.valueOf(value.toUpperCase());
					} catch (IllegalArgumentException e) {
						// ENUM名でない場合、日本語ラベルから検索（既存データの互換性のため）
						for (Gender gender : Gender.values()) {
							if (gender.getLabel().equals(value)) {
								return (X) gender;
							}
						}
						throw new IllegalArgumentException("Invalid gender value: " + value, e);
					}
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					String value = statement.getString(index);
					if (value == null) {
						return null;
					}
					// まずENUM名として試行
					try {
						return (X) Gender.valueOf(value.toUpperCase());
					} catch (IllegalArgumentException e) {
						// ENUM名でない場合、日本語ラベルから検索（既存データの互換性のため）
						for (Gender gender : Gender.values()) {
							if (gender.getLabel().equals(value)) {
								return (X) gender;
							}
						}
						throw new IllegalArgumentException("Invalid gender value: " + value, e);
					}
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					String value = statement.getString(name);
					if (value == null) {
						return null;
					}
					// まずENUM名として試行
					try {
						return (X) Gender.valueOf(value.toUpperCase());
					} catch (IllegalArgumentException e) {
						// ENUM名でない場合、日本語ラベルから検索（既存データの互換性のため）
						for (Gender gender : Gender.values()) {
							if (gender.getLabel().equals(value)) {
								return (X) gender;
							}
						}
						throw new IllegalArgumentException("Invalid gender value: " + value, e);
					}
				}
			};
		}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.OTHER;
	}
}
