package com.example.fitnessgym_mg.entity.enums;

public enum PostureImagePosition {
	FRONT("front"), RIGHT("right"), BACK("back"), LEFT("left");

	private final String code;

	PostureImagePosition(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	/**
	 * コード値からEnumを取得
	 * 
	 * @param value コード値
	 * @return PostureImagePosition Enum
	 * @throws IllegalArgumentException valueがnull、または有効なコード値でない場合
	 */
	public static PostureImagePosition fromCode(String value) {
		if (value == null) {
			throw new IllegalArgumentException("PostureImagePosition code cannot be null");
		}
		for (PostureImagePosition position : values()) {
			if (position.code.equalsIgnoreCase(value)) {
				return position;
			}
		}
		throw new IllegalArgumentException("Unknown posture image position: " + value);
	}

	/**
	 * デバッグ用にEnum名を返す
	 * <p>DB用のコード値が必要な場合は、{@link #getCode()}を使用してください。</p>
	 * 
	 * @return Enum名（"FRONT", "RIGHT", "BACK", "LEFT"）
	 */
	@Override
	public String toString() {
		return name();
	}
}
