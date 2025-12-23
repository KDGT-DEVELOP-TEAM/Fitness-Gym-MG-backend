package com.example.fitnessgym_mg.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.example.fitnessgym_mg.dto.response.PostureImageResponse;
import com.example.fitnessgym_mg.dto.response.TrainingResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse.ChartSeries;
import com.example.fitnessgym_mg.dto.response.LessonResponse.LessonChartData;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

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

	// --- レッスン一覧の検索と絞り込み (Pageable対応に修正) ---
	// ★ Pageable を引数に追加し、戻り値を Page に変更 ★
	@Transactional(readOnly = true)
	public Page<LessonResponse> searchLessons(UUID storeId, String keyword, Pageable pageable) {

		Page<Lesson> lessonPage;
		LocalDateTime now = LocalDateTime.now();

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

	// --- レッスン回数グラフデータの作成 ---
	@Transactional(readOnly = true)
	public LessonChartData getLessonChartData(UUID storeId, String type) {
		LocalDateTime now = LocalDateTime.now();
		UUID storeUuid = storeId;

		// 1. 期間タイプの決定とJPQL呼び出し
		String intervalType;
		if ("week".equals(type)) {
			intervalType = "week";
		} else {
			intervalType = "month";
			type = "month";
		}

		// DBから集計結果を取得 [0: 期間開始日時, 1: 回数]
		List<Object[]> rawChartData = lessonRepository.countLessonsGroupedByPeriod(
				intervalType, now, storeUuid);

		return buildChartData(rawChartData, type);
	}

	// --- 新規レッスン作成 ---
	@Transactional
	public Lesson createLesson(LessonRequest request) {
		// エンティティの取得
		LessonEntities entities = prepareLessonEntities(request);
		
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
		
		// トレーニング取得
		List<TrainingResponse> trainings = trainingService.getTrainingsByLessonId(lessonId);
		
		// 姿勢画像取得
		List<PostureGroup> postureGroups = postureGroupRepository.findByLessonIdOrderByCapturedAtDesc(lessonId);
		List<PostureImageResponse> postureImages = postureGroups.stream()
			.flatMap(pg -> pg.getImages().stream())
			.map(PostureImageResponse::fromEntity)
			.collect(Collectors.toList());
		
		// レスポンス作成（fromEntityを使用して基本データを設定）
		LessonResponse response = LessonResponse.fromEntity(lesson);
		
		// 追加データを設定
		response.setCondition(lesson.getCondition());
		response.setWeight(lesson.getWeight());
		response.setBmi(LessonResponse.calculateBmi(lesson.getWeight(), lesson.getCustomer().getHeight()));
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
		return lessonRepository.findByCustomerIdOrderByStartDateDesc(customerId).stream()
				.map(LessonResponse::fromEntity)
				.collect(Collectors.toList());
	}

	/**
	 * 顧客IDに紐づくレッスン履歴をページネーション対応で取得
	 */
	@Transactional(readOnly = true)
	public Page<LessonResponse> getLessonsByCustomerId(UUID customerId, Pageable pageable) {
		Page<Lesson> lessonPage = lessonRepository.findByCustomerIdOrderByStartDateDesc(customerId, pageable);
		return lessonPage.map(LessonResponse::fromEntity);
	}

	/**
	 * 顧客IDでレッスン回数グラフデータの作成
	 */
	@Transactional(readOnly = true)
	public LessonChartData getLessonChartDataByCustomerId(UUID customerId, String type) {
		LocalDateTime now = LocalDateTime.now();

		// 1. 期間タイプの決定
		String intervalType;
		if ("week".equals(type)) {
			intervalType = "week";
		} else {
			intervalType = "month";
			type = "month";
		}

		// DBから集計結果を取得 [0: 期間開始日時, 1: 回数]
		List<Object[]> rawChartData = lessonRepository.countLessonsGroupedByPeriodByCustomerId(
				intervalType, now, customerId);

		return buildChartData(rawChartData, type);
	}

	/**
	 * グラフデータの構築（共通ロジック）
	 */
	private LessonChartData buildChartData(List<Object[]> rawChartData, String type) {
		List<ChartSeries> series = new ArrayList<>();
		int maxCount = 0;

		for (Object[] row : rawChartData) {
			// PostgreSQLはTIMESTAMP型またはInstant型を返す可能性があるため、安全に変換
			LocalDateTime periodStartAt;
			Object periodObj = row[0];
			if (periodObj instanceof java.sql.Timestamp) {
				periodStartAt = ((java.sql.Timestamp) periodObj).toInstant()
						.atZone(ZoneId.systemDefault())
						.toLocalDateTime();
			} else if (periodObj instanceof java.time.Instant) {
				periodStartAt = ((java.time.Instant) periodObj)
						.atZone(ZoneId.systemDefault())
						.toLocalDateTime();
			} else if (periodObj instanceof java.time.OffsetDateTime) {
				periodStartAt = ((java.time.OffsetDateTime) periodObj)
						.toLocalDateTime();
			} else {
				// その他の型の場合は文字列として扱うか、エラーをスロー
				throw new RuntimeException("Unsupported timestamp type: " + periodObj.getClass().getName());
			}

			long count = ((Number) row[1]).longValue();

			// ラベルの生成
			String label;
			if ("week".equals(type)) {
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

			// maxCountの更新
			int currentCount = (int) count;
			if (currentCount > maxCount) {
				maxCount = currentCount;
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
		chartData.setType(type);
		return chartData;
	}

	/**
	 * トレーナーIDで直近1週間以内（当日含む）のレッスンを取得
	 * 当日・直近(1週間以内)の予約状況/レッスン概要の取得用
	 */
	@Transactional(readOnly = true)
	public List<LessonResponse> getUpcomingLessonsByTrainerId(UUID trainerId) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime oneWeekLater = now.plusWeeks(1);
		
		// 開始日時が現在から1週間以内のレッスンを取得
		List<Lesson> lessons = lessonRepository.findUpcomingLessonsByTrainerId(
				trainerId, now);
		
		// 1週間以内に限定
		return lessons.stream()
				.filter(lesson -> lesson.getStartDate() != null && lesson.getStartDate().isBefore(oneWeekLater))
				.map(LessonResponse::fromEntity)
				.collect(Collectors.toList());
	}

	/**
	 * 顧客IDで体重/BMI履歴を取得
	 * レッスンデータから体重とBMIの時系列データを取得
	 */
	@Transactional(readOnly = true)
	public List<com.example.fitnessgym_mg.dto.response.VitalsHistoryResponse.VitalsData> getVitalsHistoryByCustomerId(UUID customerId) {
		List<Lesson> lessons = lessonRepository.findByCustomerIdOrderByStartDateDesc(customerId);
		
		return lessons.stream()
				.filter(lesson -> lesson.getWeight() != null && lesson.getStartDate() != null && lesson.getCustomer() != null)
				.map(lesson -> {
					java.math.BigDecimal bmi = LessonResponse.calculateBmi(lesson.getWeight(), lesson.getCustomer().getHeight());
					return com.example.fitnessgym_mg.dto.response.VitalsHistoryResponse.VitalsData.builder()
							.date(lesson.getStartDate())
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
		Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンが見つかりません"));

		// エンティティの取得
		LessonEntities entities = prepareLessonEntities(request);
		
		// レッスン情報を更新
		applyLessonRequestToEntity(lesson, request, entities);
		
		// レッスン保存
		Lesson savedLesson = lessonRepository.save(lesson);
		
		// トレーニング更新（既存を削除して新規作成）
		if (request.getTrainings() != null) {
			trainingService.deleteByLessonId(lessonId);
			if (!request.getTrainings().isEmpty()) {
				trainingService.createTrainings(savedLesson.getId(), request.getTrainings());
			}
		}
		
		// レスポンスを返す
		return getLessonDetail(savedLesson.getId());
	}

	/**
	 * レッスンリクエストから必要なエンティティを取得する共通メソッド
	 */
	private LessonEntities prepareLessonEntities(LessonRequest request) {
		Customer customer = customerRepository.findById(request.getCustomerId())
			.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません"));
		Store store = storeRepository.findById(request.getStoreId())
			.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("店舗が見つかりません"));
		User trainer = userRepository.findById(request.getTrainerId())
			.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("トレーナーが見つかりません"));
		
		// 次回店舗・トレーナー（任意）
		Store nextStore = request.getNextStoreId() != null 
			? storeRepository.findById(request.getNextStoreId()).orElse(null) 
			: null;
		User nextTrainer = request.getNextTrainerId() != null 
			? userRepository.findById(request.getNextTrainerId()).orElse(null) 
			: null;
		
		return new LessonEntities(customer, store, trainer, nextStore, nextTrainer);
	}

	/**
	 * レッスンリクエストの内容をレッスンエンティティに適用する共通メソッド
	 */
	private void applyLessonRequestToEntity(Lesson lesson, LessonRequest request, LessonEntities entities) {
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
			User nextTrainer
	) {}

	/**
	 * レッスンフォーム表示用のデータを準備する
	 * ビジネスロジックをサービス層に集約
	 * 
	 * @param customerId 顧客ID
	 * @param storeId 店舗ID（店長の場合に必要）
	 * @param requestPath リクエストパス（ユーザータイプ判定用）
	 * @return レッスンフォームデータ
	 */
	@Transactional(readOnly = true)
	public LessonFormData prepareLessonFormData(UUID customerId, UUID storeId, String requestPath) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません: " + customerId));
		
		boolean isTrainer = requestPath != null && requestPath.startsWith("/customer/");
		
		List<Store> stores;
		List<User> trainers;
		
		if (isTrainer) {
			// トレーナーの場合：ログインユーザーの所属店舗のみ
			User currentUser = securityUtil.getCurrentUserOrThrow();
			stores = currentUser.getStores() != null && !currentUser.getStores().isEmpty()
					? new java.util.ArrayList<>(currentUser.getStores())
					: List.of();
			trainers = List.of(currentUser);
		} else if (storeId != null) {
			// 店長の場合：所属店舗のみ
			stores = List.of(storeRepository.findById(storeId)
					.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("店舗が見つかりません: " + storeId)));
			trainers = userRepository.findAll();
		} else {
			// 管理者の場合：全店舗
			stores = storeRepository.findAll();
			trainers = userRepository.findAll();
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
			boolean isTrainer
	) {}
}