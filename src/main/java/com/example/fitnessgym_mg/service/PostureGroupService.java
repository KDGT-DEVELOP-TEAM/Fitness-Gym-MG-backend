package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像グループサービスクラス
 * 姿勢画像グループの取得処理を担当
 */
@Service
@RequiredArgsConstructor
public class PostureGroupService {

    private final PostureGroupRepository postureGroupRepository;

    /**
     * 指定された顧客IDに紐づく姿勢画像グループ一覧を取得
     * 画像情報も含めて取得
     * @param customerId 顧客ID
     * @return 姿勢画像グループのリスト
     */
    @Transactional(readOnly = true)
    public List<PostureGroup> findByCustomerId(UUID customerId) {
        return postureGroupRepository.findAllWithImagesByCustomerId(customerId);
    }

    /**
     * 姿勢画像グループIDから姿勢画像グループを取得
     * @param postureGroupId 姿勢画像グループID
     * @return 姿勢画像グループ
     * @throws ResponseStatusException 見つからない場合
     */
    @Transactional(readOnly = true)
    public PostureGroup findById(UUID postureGroupId) {
        return postureGroupRepository.findById(postureGroupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Posture group not found: " + postureGroupId));
    }
}

