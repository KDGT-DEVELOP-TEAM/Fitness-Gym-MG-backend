package com.example.fitnessgym_mg.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.LessonRequest;
import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse.ChartSeries;
import com.example.fitnessgym_mg.dto.response.LessonResponse.LessonChartData;
import com.example.fitnessgym_mg.dto.response.PostureImageResponse;
import com.example.fitnessgym_mg.dto.response.TrainingResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.ChartPeriod;
import com.example.fitnessgym_mg.entity.enums.LessonFormCaller;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.exception.AccessDeniedException;
import com.example.fitnessgym_mg.exception.InvalidRequestException;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.repository.dto.PeriodCount;
import com.example.fitnessgym_mg.util.BmiCalculator;
import com.example.fitnessgym_mg.util.DateTimeUtil;
import com.example.fitnessgym_mg.util.PageableValidator;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * レッスン関連のビジネスロジックを提供するService
 * 
 * <p><b>認可方針:</b>
 * <ul>
 *   <li><b>リソース認可（AuthorizationFacade経由）:</b> 顧客・レッスンなど"個体"に紐づくものは、AuthorizationFacadeを使用してアクセス権限を確認する。
 *       例: getLessonDetail, getLessonsByCustomerId, createLesson, updateLesson など</li>
 *   <li><b>役割認可（Service層で直接チェック）:</b> 画面全体・一覧・集計など、特定のリソースに紐づかない操作は、Service層で役割ベースの認可を実施する。
 *       例: searchLessons, getLessonChartData など</li>
 * </ul>
 * Service層は最終防衛ラインとして機能し、Controller層の前提に依存しない設計を維持する。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

	private final LessonRepository lessonRepository;
	private final TrainingService trainingService;
	private final PostureGroupRepository postureGroupRepository;
	private final CustomerRepository customerRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;
	private final SecurityUtil securityUtil;
	private final AuthorizationFacade authorizationFacade;
	private final StorageService storageService;

	/**
	 * レッスン一覧の検索と絞り込み
	 * 
	 * <p>認可方針: 管理者・店長専用API。Service層で役割ベース認可を実施。
	 * storeIdがnullの場合の全店舗検索はADMIN権限を確認する。</p>
	 * 
	 * @param storeId 店舗ID（nullの場合は全店舗検索）
	 * @param pageable ページネーション情報
	 * @return レッスン一覧
	 */
	@Transactional(readOnly = true)
	public Page<LessonResponse> searchLessons(UUID storeId, Pageable pageable) {
		// 認可チェック: Service層での最終防衛ライン
		User currentUser = securityUtil.getCurrentUserOrThrow();
		if (!currentUser.getRoles().contains(UserRole.ADMIN) && !currentUser.getRoles().contains(UserRole.MANAGER)) {
			throw new AccessDeniedException("この操作を実行する権限がありません");
		}

		// offset検証: DoS対策として巨大なOFFSETクエリを防ぐ
		PageableValidator.validateOffset(pageable);

		Page<Lesson> lessonPage;
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		// ソートは Repository メソッド名で定義されているため、Pageableにはサイズとページ番号のみを渡す
		// findByStoreIdAndEndDateBefore... (ソート済み) を使用するため、Pageableはソート情報なしでOK

		// 1. 絞り込み (店舗ID + 終了日時)
		if (storeId != null) {
			// 店舗IDで絞り込み
			lessonPage = lessonRepository.findByStoreIdAndEndDateBefore(storeId, now, pageable);
		} else {
			// 店舗絞り込みなし
			lessonPage = lessonRepository.findByEndDateBefore(now, pageable);
		}

		// 2. マッピング
		return lessonPage.map(LessonResponse::fromEntity);
	}

	/**
	 * レッスン回数グラフデータの作成
	 * 
	 * <p>認可方針: 管理者・店長専用API。Service層で役割ベース認可を実施。</p>
	 * 
	 * @param storeId 店舗ID
	 * @param period 期間タイプ（WEEK or MONTH）
	 * @return グラフデータ
	 */
	@Transactional(readOnly = true)
	public LessonChartData getLessonChartData(UUID storeId, ChartPeriod period) {
		// 認可チェック: Service層での最終防衛ライン
		User currentUser = securityUtil.getCurrentUserOrThrow();
		if (!currentUser.getRoles().contains(UserRole.ADMIN) && !currentUser.getRoles().contains(UserRole.MANAGER)) {
			throw new AccessDeniedException("この操作を実行する権限がありません");
		}

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		UUID storeUuid = storeId;

		// 1. 期間タイプの決定とJPQL呼び出し
		String intervalType = (period == ChartPeriod.WEEK) ? "week" : "month";

		// DBから集計結果を取得（DTO Projectionを使用）
		List<PeriodCount> rawChartData = lessonRepository.countLessonsGroupedByPeriod(
				intervalType, now, storeUuid);

		return buildChartData(rawChartData, period);
	}

	/**
	 * 新規レッスン作成
	 * 
	 * <p>認可方針: Service層で顧客へのアクセス権限を確認。Service層が最終防衛ラインとして機能する。</p>
	 * 
	 * @param customerId 顧客ID
	 * @param request レッスンリクエスト
	 * @return 作成されたレッスンエンティティ
	 */
	@Transactional
	public Lesson createLesson(UUID customerId, LessonRequest request) {
		// 認可チェック: Service層での最終防衛ライン
		User currentUser = securityUtil.getCurrentUserOrThrow();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// エンティティの取得
		LessonEntities entities = prepareLessonEntities(customerId, request);

		// レッスンエンティティの作成
		Lesson lesson = new Lesson();
		applyLessonRequestToEntity(lesson, request, entities);

		// レッスン保存
		Lesson savedLesson = lessonRepository.save(lesson);

		// トレーニング保存
		if (request.getTrainings() != null && !request.getTrainings().isEmpty()) {
			trainingService.createTrainings(savedLesson.getId(), request.getTrainings());
		}

		return savedLesson;
	}

	// --- レッスン詳細取得 ---
	@Transactional(readOnly = true)
	public LessonResponse getLessonDetail(UUID lessonId) {
		Lesson lesson = lessonRepository.findByIdWithRelations(lessonId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンが見つかりません"));

		// 関連エンティティのnullチェック
		if (lesson.getCustomer() == null) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンに顧客情報が紐づいていません");
		}
		if (lesson.getTrainer() == null) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンにトレーナー情報が紐づいていません");
		}
		if (lesson.getStore() == null) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンに店舗情報が紐づいていません");
		}

		// 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		UUID customerId = lesson.getCustomer().getId();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// トレーニングと姿勢画像の取得（順次実行、N+1問題を回避するためJOIN FETCHを使用）
		List<TrainingResponse> trainings = trainingService.getTrainingsByLessonId(lessonId);
		List<PostureGroup> postureGroups = postureGroupRepository.findByLessonIdWithImages(lessonId);
		
		// 姿勢画像をレスポンスに変換し、署名付きURLも生成（並列化）
		int expiresIn = com.example.fitnessgym_mg.config.ApplicationConstants.DEFAULT_SIGNED_URL_EXPIRES_IN;
		List<PostureImageResponse> postureImages = postureGroups.stream()
				.flatMap(pg -> pg.getImages().stream())
				.parallel() // 並列ストリームに変換して署名付きURL生成を並列化
				.map(entity -> {
					PostureImageResponse response = PostureImageResponse.fromEntity(entity);
					// 署名付きURLを生成して設定（並列実行）
					try {
						String signedUrl = storageService.generateSignedUrl(entity.getStorageKey(), expiresIn);
						response.setSignedUrl(signedUrl);
					} catch (Exception e) {
						log.warn("Failed to generate signed URL for image: {}", entity.getId(), e);
						// 署名付きURLの生成に失敗しても続行（URLなしで表示）
					}
					return response;
				})
				.collect(Collectors.toList());

		// レスポンス作成（fromEntityを使用して基本データを設定）
		LessonResponse response = LessonResponse.fromEntity(lesson);

		// 追加データを設定
		response.setCondition(lesson.getCondition());
		response.setWeight(lesson.getWeight());
		response.setBmi(BmiCalculator.calculate(lesson.getWeight(), lesson.getCustomer().getHeight()));
		response.setMeal(lesson.getMeal());
		response.setMemo(lesson.getMemo());
		response.setNextDate(lesson.getNextDate());
		response.setNextStoreName(lesson.getNextStore() != null ? lesson.getNextStore().getName() : null);
		response.setNextTrainerName(lesson.getNextUser() != null ? lesson.getNextUser().getName() : null);
		response.setTrainings(trainings);
		response.setPostureImages(postureImages);

		return response;
	}

	// --- 顧客IDでレッスン履歴を取得 ---
	/**
	 * 顧客IDに紐づくレッスン履歴を開始日時の降順で取得し、LessonResponseに変換
	 */
	@Transactional(readOnly = true)
	public List<LessonResponse> getLessonsByCustomerId(UUID customerId) {
		// 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// Customer情報を取得（BMI計算に必要）
		Customer customer = customerRepository.findById(customerId)
				.orElse(null);
		java.math.BigDecimal customerHeight = customer != null ? customer.getHeight() : null;

		// レッスン一覧を取得
		List<Lesson> lessons = lessonRepository.findByCustomerIdOrderByStartDateDesc(customerId);

		// LessonResponseに変換し、weightとbmiを設定
		return lessons.stream()
				.map(lesson -> {
					LessonResponse response = LessonResponse.fromEntity(lesson);
					
					// Customer情報を設定
					if (customer != null) {
						response.setCustomerId(customer.getId());
						response.setCustomerName(customer.getName());
					}
					
					// weightとbmiを設定（BMI計算に必要）
					response.setWeight(lesson.getWeight());
					if (lesson.getWeight() != null && customerHeight != null) {
						java.math.BigDecimal bmi = BmiCalculator.calculate(lesson.getWeight(), customerHeight);
						response.setBmi(bmi);
					}
					
					// 次回レッスン情報を設定
					if (lesson.getNextDate() != null) {
						response.setNextDate(lesson.getNextDate());
					}
					if (lesson.getNextStore() != null) {
						response.setNextStoreName(lesson.getNextStore().getName());
					}
					if (lesson.getNextUser() != null) {
						response.setNextTrainerName(lesson.getNextUser().getName());
					}
					
					return response;
				})
				.collect(Collectors.toList());
	}

	/**
	 * 顧客IDに紐づくレッスン履歴をページネーション対応で取得
	 * CustomerをJOIN FETCHしないことで@SQLRestrictionを回避し、Customer情報はネイティブSQLクエリで別途取得
	 */
	@Transactional(readOnly = true)
	public Page<LessonResponse> getLessonsByCustomerId(UUID customerId, Pageable pageable) {
		// 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// offset検証: DoS対策として巨大なOFFSETクエリを防ぐ
		PageableValidator.validateOffset(pageable);

		// CustomerをJOIN FETCHしないことで@SQLRestrictionを回避
		Page<Lesson> lessonPage = lessonRepository.findByCustomerIdOrderByStartDateDesc(customerId, pageable);

		// Customer情報を1回のクエリで取得（すべてのレッスンが同じcustomerIdを持つため）
		Customer customer = customerRepository.findById(customerId)
				.orElse(null);
		
		UUID customerIdForResponse = customer != null ? customer.getId() : customerId;
		String customerNameForResponse = customer != null ? customer.getName() : null;

		// LessonResponseに変換し、Customer情報を設定
		return lessonPage.map(lesson -> {
			LessonResponse response = LessonResponse.fromEntity(lesson);
			
			// Customer情報を設定（全レッスンが同じcustomerIdを持つため）
			if (customerIdForResponse != null) {
				response.setCustomerId(customerIdForResponse);
			}
			if (customerNameForResponse != null) {
				response.setCustomerName(customerNameForResponse);
			}
			
			// weightとbmiを設定（BMI計算に必要）
			response.setWeight(lesson.getWeight());
			if (lesson.getWeight() != null && customer != null && customer.getHeight() != null) {
				java.math.BigDecimal bmi = BmiCalculator.calculate(lesson.getWeight(), customer.getHeight());
				response.setBmi(bmi);
			}
			
			// 次回レッスン情報を設定
			if (lesson.getNextDate() != null) {
				response.setNextDate(lesson.getNextDate());
			}
			if (lesson.getNextStore() != null) {
				response.setNextStoreName(lesson.getNextStore().getName());
			}
			if (lesson.getNextUser() != null) {
				response.setNextTrainerName(lesson.getNextUser().getName());
			}
			
			return response;
		});
	}

	/**
	 * 顧客IDでレッスン回数グラフデータの作成
	 * 
	 * <p>認可方針: Service層で顧客へのアクセス権限を確認。</p>
	 * 
	 * @param customerId 顧客ID
	 * @param period 期間タイプ（WEEK or MONTH）
	 * @return グラフデータ
	 */
	@Transactional(readOnly = true)
	public LessonChartData getLessonChartDataByCustomerId(UUID customerId, ChartPeriod period) {
		// 認可チェック: Service層での最終防衛ライン
		User currentUser = securityUtil.getCurrentUserOrThrow();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		// 1. 期間タイプの決定
		String intervalType = (period == ChartPeriod.WEEK) ? "week" : "month";

		// DBから集計結果を取得（DTO Projectionを使用）
		List<PeriodCount> rawChartData = lessonRepository.countLessonsGroupedByPeriodByCustomerId(
				intervalType, now, customerId);

		return buildChartData(rawChartData, period);
	}

	/**
	 * グラフデータの構築（共通ロジック）
	 * 
	 * <p>DTO Projectionを使用し、Repositoryの返却型への依存を排除。</p>
	 * 
	 * @param rawChartData 集計結果データ
	 * @param period 期間タイプ（WEEK or MONTH）
	 * @return グラフデータ
	 */
	private LessonChartData buildChartData(List<PeriodCount> rawChartData, ChartPeriod period) {
		List<ChartSeries> series = new ArrayList<>();
		long maxCount = 0L;

		for (PeriodCount row : rawChartData) {
			// DTO Projectionにより、型安全にアクセス可能
			OffsetDateTime periodStartAt = row.periodStart();
			Long countValue = row.count();
			// nullチェック: COALESCEで0を保証しているが、防御的プログラミングとしてnullチェックを実装
			long count = (countValue != null) ? countValue : 0L;

			// ラベルの生成
			// OffsetDateTimeからLocalDateに変換（タイムゾーン情報を保持したまま）
			String label;
			if (period == ChartPeriod.WEEK) {
				// PostgreSQLの date_trunc('week') は通常、月曜日を返す（ただし設定依存）。
				// Java側でラベル整形を行う
				LocalDate startDate = periodStartAt.toLocalDate();
				LocalDate endDate = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				label = startDate.getMonthValue() + "/" + startDate.getDayOfMonth() +
						" - " + endDate.getMonthValue() + "/" + endDate.getDayOfMonth();
			} else {
				// 月別
				label = periodStartAt.getYear() + "/" + periodStartAt.getMonthValue();
			}

			// maxCountの更新（型の一貫性を確保）
			if (count > maxCount) {
				maxCount = count;
			}

			ChartSeries chartSeries = new ChartSeries();
			chartSeries.setPeriod(label);
			chartSeries.setCount(count);
			series.add(chartSeries);
		}

		// グラフの要件に従い、「右が最新」にするため、リストを逆順にする
		java.util.Collections.reverse(series);

		LessonChartData chartData = new LessonChartData();
		chartData.setSeries(series);
		chartData.setMaxCount(maxCount);
		chartData.setType(period == ChartPeriod.WEEK ? "week" : "month");
		return chartData;
	}

	/**
	 * トレーナーIDで直近1週間以内（当日含む）のレッスンを取得
	 * 当日・直近(1週間以内)の予約状況/レッスン概要の取得用
	 * 
	 * <p>認可方針: Service層で自己参照（自分のIDのみ）を検証。</p>
	 * 
	 * @param trainerId トレーナーID
	 * @return 直近1週間以内のレッスン一覧
	 */
	@Transactional(readOnly = true)
	public List<LessonResponse> getUpcomingLessonsByTrainerId(UUID trainerId) {
		// 認可チェック: Service層での最終防衛ライン（自己参照の検証）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		if (!currentUser.getId().equals(trainerId)) {
			throw new AccessDeniedException("自分のレッスンのみアクセス可能です");
		}

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		LocalDateTime oneWeekLater = now.plusWeeks(1);

		// 開始日時が現在から1週間以内のレッスンを取得（Repositoryで範囲検索）
		List<Lesson> lessons = lessonRepository.findUpcomingLessonsByTrainerIdBetween(
				trainerId, now, oneWeekLater);

		return lessons.stream()
				.map(LessonResponse::fromEntity)
				.collect(Collectors.toList());
	}

	/**
	 * トレーナーIDで1週間後～1ヶ月後までのレッスンを取得（ページネーション対応）
	 * トレーナーホームページ用
	 * 
	 * <p>認可方針: Service層で自己参照（自分のIDのみ）を検証。</p>
	 * 
	 * @param trainerId トレーナーID
	 * @param pageable ページネーション情報
	 * @return 1週間後～1ヶ月後までのレッスン一覧（ページネーション）
	 */
	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<LessonResponse> getUpcomingLessonsByTrainerId(UUID trainerId, org.springframework.data.domain.Pageable pageable) {
		// 認可チェック: Service層での最終防衛ライン（自己参照の検証）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		if (!currentUser.getId().equals(trainerId)) {
			throw new AccessDeniedException("自分のレッスンのみアクセス可能です");
		}

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		LocalDateTime oneWeekLater = now.plusWeeks(1);
		LocalDateTime oneMonthLater = now.plusMonths(1);

		// 1週間後～1ヶ月後のレッスンを取得（ページネーション対応）
		org.springframework.data.domain.Page<Lesson> lessonPage = lessonRepository.findUpcomingLessonsByTrainerIdBetween(
				trainerId, oneWeekLater, oneMonthLater, pageable);

		return lessonPage.map(LessonResponse::fromEntity);
	}

	/**
	 * 次回トレーナーIDで1週間後～1ヶ月後までの次回レッスン希望を取得（ページネーション対応）
	 * トレーナーホームページ用
	 * 
	 * <p>認可方針: Service層で自己参照（自分のIDのみ）を検証。</p>
	 * 
	 * @param trainerId 次回トレーナーID（nextUser.id）
	 * @param pageable ページネーション情報
	 * @return 1週間後～1ヶ月後までの次回レッスン希望一覧（ページネーション）
	 */
	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<LessonResponse> getNextLessonsByTrainerId(UUID trainerId, org.springframework.data.domain.Pageable pageable) {
		// 認可チェック: Service層での最終防衛ライン（自己参照の検証）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		if (!currentUser.getId().equals(trainerId)) {
			throw new AccessDeniedException("自分のレッスンのみアクセス可能です");
		}

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		LocalDateTime oneWeekLater = now.plusWeeks(1);
		LocalDateTime oneMonthLater = now.plusMonths(1);

		// 次回レッスン希望を取得（ページネーション対応）
		org.springframework.data.domain.Page<Lesson> lessonPage = lessonRepository.findNextLessonsByNextTrainerIdBetween(
				trainerId, oneWeekLater, oneMonthLater, pageable);

		// CustomerはJOIN FETCHしていないため、Customerの情報をバッチで取得（N+1問題を回避）
		java.util.List<Lesson> lessons = lessonPage.getContent();
		java.util.Map<UUID, java.util.Map<String, Object>> customerMap = new java.util.HashMap<>();
		
		if (!lessons.isEmpty()) {
			// レッスンIDのリストを作成
			java.util.List<UUID> lessonIds = lessons.stream()
					.map(Lesson::getId)
					.collect(Collectors.toList());
			
			try {
				// バッチでCustomer情報を取得（ネイティブSQLクエリを使用して@SQLRestrictionを回避）
				java.util.List<Object[]> customerDataList = lessonRepository.findCustomerIdAndNameByLessonIds(lessonIds);
				
				log.debug("Customer情報取得: lessonIds={}, customerDataList.size()={}", lessonIds.size(), customerDataList.size());
				
				// レッスンIDをキーとしてCustomer情報をマップに格納
				for (Object[] row : customerDataList) {
					try {
						// ネイティブSQLクエリの結果は、PostgreSQLではUUIDがObjectとして返される可能性がある
						// 型変換を安全に行う
						UUID lessonId = convertToUUID(row[0]);
						UUID customerId = convertToUUID(row[1]);
						String customerName = row[2] != null ? row[2].toString() : null;
						
						if (lessonId != null && customerId != null && customerName != null) {
							java.util.Map<String, Object> customerInfo = new java.util.HashMap<>();
							customerInfo.put("id", customerId);
							customerInfo.put("name", customerName);
							customerMap.put(lessonId, customerInfo);
						}
					} catch (Exception e) {
						log.warn("Customer情報のマッピングに失敗: row={}, error={}", java.util.Arrays.toString(row), e.getMessage());
					}
				}
			} catch (Exception e) {
				log.error("Customer情報の取得に失敗: lessonIds={}, error={}", lessonIds, e.getMessage(), e);
				// エラーが発生しても処理を続行（Customer情報なしでレスポンスを返す）
			}
		}

		// LessonResponseに変換（nextDate, nextStoreName, nextTrainerNameも含める）
		return lessonPage.map(lesson -> {
			LessonResponse response = LessonResponse.fromEntity(lesson);
			
			// Customerの情報をマップから取得して設定
			java.util.Map<String, Object> customerInfo = customerMap.get(lesson.getId());
			if (customerInfo != null) {
				response.setCustomerId((UUID) customerInfo.get("id"));
				response.setCustomerName((String) customerInfo.get("name"));
			}
			
			// 次回レッスン情報を設定
			if (lesson.getNextDate() != null) {
				response.setNextDate(lesson.getNextDate());
			}
			if (lesson.getNextStore() != null) {
				response.setNextStoreName(lesson.getNextStore().getName());
			}
			if (lesson.getNextUser() != null) {
				response.setNextTrainerName(lesson.getNextUser().getName());
			}
			return response;
		});
	}

	/**
	 * 顧客IDで体重/BMI履歴を取得
	 * レッスンデータから体重とBMIの時系列データを取得
	 */
	@Transactional(readOnly = true)
	public List<com.example.fitnessgym_mg.dto.response.VitalsHistoryResponse.VitalsData> getVitalsHistoryByCustomerId(
			UUID customerId) {
		// 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// Customer情報を1回のクエリで取得（N+1問題を回避）
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません"));
		java.math.BigDecimal customerHeight = customer.getHeight();

		// レッスン一覧を取得（CustomerはJOIN FETCHしない）
		List<Lesson> lessons = lessonRepository.findByCustomerIdOrderByStartDateDesc(customerId);

		// Customer情報を使用してBMIを計算（N+1問題を回避）
		return lessons.stream()
				.filter(lesson -> lesson.getWeight() != null && lesson.getStartDate() != null)
				.map(lesson -> {
					java.math.BigDecimal bmi = BmiCalculator.calculate(lesson.getWeight(), customerHeight);
					return com.example.fitnessgym_mg.dto.response.VitalsHistoryResponse.VitalsData.builder()
							.date(DateTimeUtil.toUtcOffsetAssumingUtc(lesson.getStartDate()))
							.weight(lesson.getWeight())
							.bmi(bmi)
							.build();
				})
				.collect(Collectors.toList());
	}

	/**
	 * レッスン情報の更新
	 */
	@Transactional
	public LessonResponse updateLesson(UUID lessonId, LessonRequest request) {
		// レッスンコア情報の更新
		Lesson savedLesson = updateLessonCore(lessonId, request);

		// トレーニング更新
		updateLessonTrainings(lessonId, savedLesson.getId(), request.getTrainings());

		// レスポンスを返す
		return getLessonDetail(savedLesson.getId());
	}

	/**
	 * レッスンコア情報の更新（認可チェック、エンティティ取得、部分更新、保存）
	 * 
	 * <p>トランザクション境界を持たないプライベートメソッド。
	 * 呼び出し元の@Transactionalの管理下で実行される。</p>
	 * 
	 * @param lessonId レッスンID
	 * @param request レッスンリクエスト
	 * @return 保存済みレッスンエンティティ
	 */
	private Lesson updateLessonCore(UUID lessonId, LessonRequest request) {
		// レッスンを取得（customer情報も含む）
		Lesson lesson = lessonRepository.findByIdWithRelations(lessonId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンが見つかりません"));

		// レッスンに紐づく顧客IDを取得
		if (lesson.getCustomer() == null) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンに顧客情報が紐づいていません");
		}
		UUID customerId = lesson.getCustomer().getId();

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();

		// 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// 既存のレッスンのstoreIdとtrainerIdを使用（リクエストから取得しない）
		// 次回店舗・トレーナーのエンティティ取得（任意）
		Store nextStore = request.getNextStoreId() != null
				? storeRepository.findById(request.getNextStoreId()).orElse(null)
				: null;
		User nextTrainer = request.getNextTrainerId() != null
				? userRepository.findById(request.getNextTrainerId()).orElse(null)
				: null;

		// レッスン情報を更新（storeIdとtrainerIdは既存の値を保持）
		lesson.setCondition(request.getCondition());
		lesson.setWeight(request.getWeight());
		lesson.setMeal(request.getMeal());
		lesson.setMemo(request.getMemo());
		lesson.setStartDate(request.getStartDate());
		lesson.setEndDate(request.getEndDate());
		lesson.setNextDate(request.getNextDate());
		lesson.setNextStore(nextStore);
		lesson.setNextUser(nextTrainer);
		// 注意: lesson.setCustomer(), lesson.setStore(), lesson.setTrainer()は呼び出さない

		// レッスン保存
		return lessonRepository.save(lesson);
	}

	/**
	 * レッスンのトレーニング更新（既存を削除して新規作成）
	 * 
	 * <p>トランザクション境界を持たないプライベートメソッド。
	 * 呼び出し元の@Transactionalの管理下で実行される。</p>
	 * 
	 * @param lessonId レッスンID（削除用）
	 * @param savedLessonId 保存済みレッスンID（作成用）
	 * @param trainings トレーニングリクエストリスト（nullの場合は更新しない）
	 */
	private void updateLessonTrainings(UUID lessonId, UUID savedLessonId,
			List<com.example.fitnessgym_mg.dto.request.TrainingRequest> trainings) {
		if (trainings != null) {
			trainingService.deleteByLessonId(lessonId);
			if (!trainings.isEmpty()) {
				trainingService.createTrainings(savedLessonId, trainings);
			}
		}
	}

	/**
	 * レッスンリクエストから必要なエンティティを取得する共通メソッド
	 * 
	 * <p>エンティティの存在確認と組み合わせ検証を実施する。</p>
	 */
	private LessonEntities prepareLessonEntities(UUID customerId, LessonRequest request) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません"));
		Store store = storeRepository.findById(request.getStoreId())
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("店舗が見つかりません"));
		User trainer = userRepository.findById(request.getTrainerId())
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("トレーナーが見つかりません"));

		// 組み合わせ検証: トレーナーが店舗に所属しているか、顧客が店舗に紐づいているか
		validateLessonEntityCombinations(customer, store, trainer);

		// 次回店舗・トレーナー（任意）
		Store nextStore = request.getNextStoreId() != null
				? storeRepository.findById(request.getNextStoreId()).orElse(null)
				: null;
		User nextTrainer = request.getNextTrainerId() != null
				? userRepository.findById(request.getNextTrainerId()).orElse(null)
				: null;

		// 次回店舗・トレーナーの組み合わせ検証（nullでない場合のみ）
		if (nextStore != null && nextTrainer != null) {
			// 次回トレーナーが次回店舗に所属しているか検証
			if (nextTrainer.getStores() == null || nextTrainer.getStores().isEmpty() ||
					!nextTrainer.getStores().contains(nextStore)) {
				throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
						"次回トレーナーは次回店舗に所属している必要があります");
			}
		}

		return new LessonEntities(customer, store, trainer, nextStore, nextTrainer);
	}

	/**
	 * レッスンエンティティの組み合わせ検証
	 * 
	 * <p>ビジネスルールに基づき、以下の検証を実施:</p>
	 * <ul>
	 *   <li>トレーナーが指定店舗に所属しているか</li>
	 *   <li>顧客が指定店舗に紐づいているか</li>
	 * </ul>
	 * 
	 * @param customer 顧客エンティティ
	 * @param store 店舗エンティティ
	 * @param trainer トレーナーエンティティ
	 * @throws InvalidRequestException 検証失敗の場合
	 */
	private void validateLessonEntityCombinations(Customer customer, Store store, User trainer) {
		// トレーナーが指定店舗に所属しているか検証
		if (trainer.getStores() == null || trainer.getStores().isEmpty() ||
				!trainer.getStores().contains(store)) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
					"指定されたトレーナーは指定された店舗に所属していません");
		}

		// 顧客が指定店舗に紐づいているか検証
		if (customer.getStores() == null || customer.getStores().isEmpty() ||
				!customer.getStores().contains(store)) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
					"指定された顧客は指定された店舗に紐づいていません");
		}
	}

	/**
	 * レッスンの日時範囲を検証
	 * 
	 * @param startDate 開始日時
	 * @param endDate 終了日時
	 * @throws InvalidRequestException 終了日時が開始日時より前の場合
	 */
	private void validateLessonDateRange(LocalDateTime startDate, LocalDateTime endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
					"終了日時は開始日時より後に設定してください");
		}
	}

	/**
	 * レッスンの文字列フィールドを検証
	 * 
	 * @param condition 体調
	 * @param meal 食事内容
	 * @param memo メモ
	 * @throws InvalidRequestException 文字数制限を超過している場合
	 */
	private void validateLessonStringFields(String condition, String meal, String memo) {
		if (condition != null && condition.length() > 500) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
					"体調の文字数が多すぎます（500文字以内）");
		}
		if (meal != null && meal.length() > 500) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
					"食事内容の文字数が多すぎます（500文字以内）");
		}
		if (memo != null && memo.length() > 1000) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(
					"メモの文字数が多すぎます（1000文字以内）");
		}
	}

	/**
	 * レッスンリクエストの内容をレッスンエンティティに適用する共通メソッド
	 */
	private void applyLessonRequestToEntity(Lesson lesson, LessonRequest request, LessonEntities entities) {
		// 日時範囲のバリデーション
		validateLessonDateRange(request.getStartDate(), request.getEndDate());

		// 文字列フィールドのバリデーション
		validateLessonStringFields(request.getCondition(), request.getMeal(), request.getMemo());

		lesson.setCustomer(entities.customer());
		lesson.setStore(entities.store());
		lesson.setTrainer(entities.trainer());
		lesson.setCondition(request.getCondition());
		lesson.setWeight(request.getWeight());
		lesson.setMeal(request.getMeal());
		lesson.setMemo(request.getMemo());
		lesson.setStartDate(request.getStartDate());
		lesson.setEndDate(request.getEndDate());
		lesson.setNextDate(request.getNextDate());
		lesson.setNextStore(entities.nextStore());
		lesson.setNextUser(entities.nextTrainer());
	}

	/**
	 * レッスンエンティティを保持するレコード
	 */
	private record LessonEntities(
			Customer customer,
			Store store,
			User trainer,
			Store nextStore,
			User nextTrainer) {
	}

	/**
	 * レッスンフォーム表示用のデータを準備する
	 * ビジネスロジックをサービス層に集約
	 * 
	 * <p>Service層はURL構造を知らない。Controller層で決定した呼び出し元情報をenumで受け取る。</p>
	 * 
	 * <p>認可方針: Service層で顧客へのアクセス権限を確認。Service層が最終防衛ラインとして機能する。</p>
	 * 
	 * @param customerId 顧客ID
	 * @param storeId 店舗ID（店長の場合に必要）
	 * @param caller 呼び出し元（Controller層で決定）
	 * @return レッスンフォームデータ
	 */
	@Transactional(readOnly = true)
	public LessonFormData prepareLessonFormData(UUID customerId, UUID storeId, LessonFormCaller caller) {
		// 認可チェック: Service層での最終防衛ライン
		User currentUser = securityUtil.getCurrentUserOrThrow();
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException(
						"顧客が見つかりません: " + customerId));

		List<Store> stores;
		List<User> trainers;
		boolean isTrainer;

		if (caller == LessonFormCaller.TRAINER) {
			// トレーナーの場合：ログインユーザーの所属店舗のみ
			stores = currentUser.getStores() != null && !currentUser.getStores().isEmpty()
					? new java.util.ArrayList<>(currentUser.getStores())
					: List.of();
			trainers = List.of(currentUser);
			isTrainer = true;
		} else if (caller == LessonFormCaller.MANAGER) {
			// 店長の場合：所属店舗のみ
			stores = List.of(storeRepository.findById(storeId)
					.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException(
							"店舗が見つかりません: " + storeId)));
			trainers = userRepository.findAll();
			isTrainer = false;
		} else if (caller == LessonFormCaller.ADMIN) {
			// 管理者の場合：全店舗
			stores = storeRepository.findAll();
			trainers = userRepository.findAll();
			isTrainer = false;
		} else {
			throw new IllegalArgumentException("Unknown caller: " + caller);
		}

		return new LessonFormData(customer, stores, trainers, isTrainer);
	}

	/**
	 * レッスンフォームデータを保持するレコード
	 */
	public record LessonFormData(
			Customer customer,
			List<Store> stores,
			List<User> trainers,
			boolean isTrainer) {
	}

	/**
	 * ObjectをUUIDに安全に変換するヘルパーメソッド
	 * 
	 * @param obj UUIDに変換するオブジェクト
	 * @return UUID（変換できない場合はnull）
	 */
	private UUID convertToUUID(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof UUID) {
			return (UUID) obj;
		}
		if (obj instanceof String) {
			try {
				return UUID.fromString((String) obj);
			} catch (IllegalArgumentException e) {
				log.warn("UUIDへの変換に失敗: obj={}", obj);
				return null;
			}
		}
		// PostgreSQLのネイティブクエリでは、UUIDがjava.sql.Types.OTHERとして返される可能性がある
		// toString()してからUUIDに変換を試みる
		try {
			return UUID.fromString(obj.toString());
		} catch (IllegalArgumentException e) {
			log.warn("UUIDへの変換に失敗: obj={}, obj.getClass()={}", obj, obj.getClass());
			return null;
		}
	}
}