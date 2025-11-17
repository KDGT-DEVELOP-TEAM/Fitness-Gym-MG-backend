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

@Service
@RequiredArgsConstructor
public class PostureImageService {

    private final PostureImageRepository postureImageRepository;

    @Transactional(readOnly = true)
    public List<PostureImage> findByGroupId(UUID postureGroupId) {
        return postureImageRepository.findByPostureGroupIdOrderByTakenAtAsc(postureGroupId);
    }

    @Transactional
    public void delete(UUID postureImageId) {
        if (!postureImageRepository.existsById(postureImageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Posture image not found: " + postureImageId);
        }
        postureImageRepository.deleteById(postureImageId);
    }
}

