package com.example.fitnessgym_mg.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.service.PostureImageService;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像REST APIエンドポイント
 * DB posture_imagesテーブルへの操作を提供
 */
@RestController
@RequestMapping("/api/posture_images")
@RequiredArgsConstructor
public class PostureImageController {

    private final PostureImageService postureImageService;

    /**
     * DELETE /api/posture_images/{postureImageId}
     * DBから姿勢画像レコードを削除
     * 注意: Storageの画像ファイルは削除されない
     */
    @DeleteMapping("/{postureImageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postureImageId) {
        postureImageService.delete(postureImageId);
        return ResponseEntity.noContent().build();
    }
}

