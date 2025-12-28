package com.example.fitnessgym_mg.entity.enums;

/**
 * レッスンフォーム表示時の呼び出し元を表すEnum
 * 
 * <p>Service層がURL構造を知らないようにするため、Controller層で決定して渡す。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>Service層はURL構造に依存しない</li>
 *   <li>Controller層でrequestPathやロール情報から適切な値を決定</li>
 *   <li>API/BFF/モバイル対応でも破綻しない設計</li>
 * </ul>
 */
public enum LessonFormCaller {
	/**
	 * トレーナーが呼び出し元
	 */
	TRAINER,
	
	/**
	 * 店長が呼び出し元
	 */
	MANAGER,
	
	/**
	 * 管理者が呼び出し元
	 */
	ADMIN
}

