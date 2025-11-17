package com.example.fitnessgym_mg.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.enums.Gender;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import com.example.fitnessgym_mg.service.PostureGroupService;
import com.example.fitnessgym_mg.service.PostureImageService;

@WebMvcTest({PostureGroupController.class, PostureImageController.class, PostureViewController.class})
class PostureGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostureGroupService postureGroupService;

    @MockBean
    private PostureImageService postureImageService;

    @Test
    @DisplayName("姿勢画像グループ一覧APIが期待通りのJSONを返す")
    @WithMockUser(roles = "TRAINER")
    void listGroupsReturnsExpectedPayload() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .id(customerId)
                .kana("ヤマダタロウ")
                .name("山田 太郎")
                .gender(Gender.MALE)
                .email("sample@example.com")
                .phone("00000000000")
                .address("東京都千代田区")
                .birthday(OffsetDateTime.now().toLocalDate())
                .createdAt(OffsetDateTime.now())
                .active(true)
                .build();

        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .customer(customer)
                .startDate(OffsetDateTime.parse("2025-01-10T10:00:00Z"))
                .createdAt(OffsetDateTime.parse("2025-01-10T11:00:00Z"))
                .build();

        PostureImage image = PostureImage.builder()
                .id(imageId)
                .title("front")
                .storageKey("/uploads/front.jpg")
                .consentPublication(true)
                .takenAt(OffsetDateTime.parse("2025-01-10T10:05:00Z"))
                .createdAt(OffsetDateTime.parse("2025-01-10T10:06:00Z"))
                .position(PostureImagePosition.FRONT)
                .build();

        PostureGroup group = PostureGroup.builder()
                .id(groupId)
                .customer(customer)
                .lesson(lesson)
                .capturedAt(OffsetDateTime.parse("2025-01-10T10:07:00Z"))
                .createdAt(OffsetDateTime.parse("2025-01-10T10:08:00Z"))
                .images(List.of(image))
                .build();

        when(postureGroupService.findByCustomerId(customerId)).thenReturn(List.of(group));

        mockMvc.perform(get("/api/customers/{customerId}/posture_groups", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(groupId.toString()))
                .andExpect(jsonPath("$[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].images[0].id").value(imageId.toString()))
                .andExpect(jsonPath("$[0].images[0].position").value("front"));
    }

    @Test
    @DisplayName("姿勢画像の削除APIが204を返す")
    @WithMockUser(roles = "MANAGER")
    void deleteImageReturnsNoContent() throws Exception {
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/posture_images/{imageId}", imageId))
                .andExpect(status().isNoContent());
    }
}


