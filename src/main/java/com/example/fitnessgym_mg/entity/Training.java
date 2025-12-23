package com.example.fitnessgym_mg.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DB trainingsテーブルとマッピングするエンティティ
 * レッスン内で実施されるトレーニング種目を表す
 * 複合主キー（lesson_id + order_no）を使用
 */
@Entity
@Table(name = "trainings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "lesson" })
public class Training {

	/**
	 * 複合主キー（lesson_idとorder_noの組み合わせ）
	 */
	@EqualsAndHashCode.Include
	@EmbeddedId
	private TrainingId id;

	/**
	 * レッスン（必須リレーション、insertable/updatable=falseで複合主キーとの整合性を保つ）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", insertable = false, updatable = false, nullable = false)
	private Lesson lesson;

	/**
	 * トレーニング種目名
	 */
	@Column(nullable = false, length = 100)
	private String name;

	/**
	 * 実施回数
	 */
	@Column(nullable = false)
	private Integer reps;

	/**
	 * 複合主キークラス
	 * lesson_idとorder_noの組み合わせで主キーを構成
	 */
	@Embeddable
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TrainingId implements Serializable {

		@Column(name = "lesson_id", nullable = false)
		private UUID lessonId;

		@Column(name = "order_no", nullable = false)
		private Integer orderNo;

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof TrainingId))
				return false;
			TrainingId that = (TrainingId) o;
			return lessonId.equals(that.lessonId) &&
					orderNo.equals(that.orderNo);
		}

		@Override
		public int hashCode() {
			int result = lessonId.hashCode();
			result = 31 * result + orderNo.hashCode();
			return result;
		}
	}
}
