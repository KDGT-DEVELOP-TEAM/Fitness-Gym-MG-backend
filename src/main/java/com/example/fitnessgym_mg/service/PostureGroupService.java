package com.example.fitnessgym_mg.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像グループのビジネスロジック
 * DB posture_groupsテーブルの取得処理を提供
 */
@Service
@RequiredArgsConstructor
public class PostureGroupService {

    private final PostureGroupRepository postureGroupRepository;
    private final LessonRepository lessonRepository;

    /**
     * 顧客IDで姿勢画像グループ一覧をDBから取得
     * @Transactional(readOnly = true): DBへの読み取り専用トランザクション
     */
    @Transactional(readOnly = true)
    public List<PostureGroup> findByCustomerId(UUID customerId) {
        return postureGroupRepository.findAllWithImagesByCustomerId(customerId);
    }

    /**
     * IDで姿勢画像グループをDBから取得
     * @throws ResponseStatusException DBに存在しない場合404
     */
    @Transactional(readOnly = true)
    public PostureGroup findById(UUID postureGroupId) {
        return postureGroupRepository.findById(postureGroupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Posture group not found: " + postureGroupId));
    }

    /**
     * 新しいレッスン記録に伴う、新しい姿勢画像群に対する空グループの作成
     */
    @Transactional
    public PostureGroup createPostureGroup(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lesson not found: " + lessonId));
        
        Customer customer = lesson.getCustomer();
        
        // PostureGroupエンティティの作成（@Builderがないためnewで作成）
        PostureGroup postureGroup = new PostureGroup();
        postureGroup.setCustomer(customer);
        postureGroup.setLesson(lesson);
        postureGroup.setCapturedAt(OffsetDateTime.now());
        postureGroup.setCreatedAt(OffsetDateTime.now());
        
        return postureGroupRepository.save(postureGroup);
    }
}

