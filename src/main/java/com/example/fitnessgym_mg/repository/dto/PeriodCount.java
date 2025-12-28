package com.example.fitnessgym_mg.repository.dto;

import java.time.OffsetDateTime;

/**
 * 期間別レッスン回数の集計結果を表すDTO
 * 
 * <p>Repository層のNative Queryから返却される集計結果を型安全に扱うためのDTO Projection。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>Repository層の返却型への依存を排除し、Service層を純粋なビジネス処理に専念させる</li>
 *   <li>DB変更、JPQL→Native Query変更、方言差による影響を最小化</li>
 * </ul>
 * 
 * @param periodStart 期間開始日時（UTC、タイムゾーン情報を含む）
 * @param count レッスン回数（null許容）
 */
public record PeriodCount(
		OffsetDateTime periodStart,
		Long count
) {}

