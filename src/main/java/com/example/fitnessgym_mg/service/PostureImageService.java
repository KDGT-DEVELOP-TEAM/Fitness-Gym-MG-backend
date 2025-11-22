package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.repository.PostureImageRepository;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像のビジネスロジック
 * DB posture_imagesテーブルへの操作を提供
 */
@Service
@RequiredArgsConstructor
public class PostureImageService {

    private final PostureImageRepository postureImageRepository;

    /**
     * グループIDで姿勢画像リストをDBから取得（撮影日時昇順）
     */
    @Transactional(readOnly = true)
    public List<PostureImage> findByGroupId(UUID postureGroupId) {
        return postureImageRepository.findByPostureGroupIdOrderByTakenAtAsc(postureGroupId);
    }

    /**
     * 姿勢画像をDBから削除
     * 注意: Storageの画像ファイル自体は削除されない
     * @Transactional: DBへの書き込みトランザクション
     */
    @Transactional
    public void delete(UUID postureImageId) {
        if (!postureImageRepository.existsById(postureImageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Posture image not found: " + postureImageId);
        }
        postureImageRepository.deleteById(postureImageId);
    }
}

