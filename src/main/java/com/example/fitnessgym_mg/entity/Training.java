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
import lombok.NoArgsConstructor;

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
public class Training {

	/**
	 * 複合主キー（lesson_idとorder_noの組み合わせ）
	 */
	@EmbeddedId
	private TrainingId id;

	/**
	 * レッスン（Lesson）への参照
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", insertable = false, updatable = false, nullable = false)
	private Lesson lesson;

	/**
	 * 種目名
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * 回数
	 */
	@Column(nullable = false)
	private Integer reps;

	/**
	 * 複合主キークラス
	 * lesson_idとorder_noの組み合わせで主キーを構成
	 */
	@Embeddable
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
