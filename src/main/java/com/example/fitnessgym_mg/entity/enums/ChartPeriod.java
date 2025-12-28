package com.example.fitnessgym_mg.entity.enums;

/**
 * グラフデータの期間タイプを表すEnum
 * 
 * <p>Service層が文字列判定を行わないようにするため、Controller層で変換して渡す。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>Service層は文字列判定に依存しない</li>
 *   <li>Controller層でリクエストパラメータから適切な値を決定</li>
 *   <li>API仕様の明確化と型安全性の向上</li>
 * </ul>
 */
public enum ChartPeriod {
	/**
	 * 週単位
	 */
	WEEK,
	
	/**
	 * 月単位
	 */
	MONTH
}

