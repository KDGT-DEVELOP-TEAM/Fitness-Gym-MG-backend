package com.example.fitnessgym_mg.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fitnessgym_mg.controller.api.PostureGroupApiController;
import com.example.fitnessgym_mg.controller.api.PostureImageApiController;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.Gender;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.dto.response.PostureGroupResponse;
import com.example.fitnessgym_mg.service.PostureGroupService;
import com.example.fitnessgym_mg.service.PostureImageService;
import org.mockito.ArgumentMatchers;

@WebMvcTest({PostureGroupApiController.class, PostureImageApiController.class})
class PostureGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private PostureGroupService postureGroupService;

    @SuppressWarnings("removal")
    @MockBean
    private PostureImageService postureImageService;

    @Test
    @DisplayName("姿勢画像グループ一覧APIが期待通りのJSONを返す")
    void listGroupsReturnsExpectedPayload() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Customerエンティティの作成（@Builderがないためnewで作成）
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setKana("ヤマダタロウ");
        customer.setName("山田 太郎");
        customer.setGender(Gender.MALE);
        customer.setEmail("sample@example.com");
        customer.setPhone("00000000000");
        customer.setAddress("東京都千代田区");
        customer.setBirthday(LocalDate.of(1990, 1, 1));
        customer.setHeight(BigDecimal.valueOf(170.0));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setActive(true);

        // Storeエンティティの作成（必要に応じて）
        Store store = new Store();
        store.setId(storeId);
        store.setName("テスト店舗");

        // Userエンティティの作成（必要に応じて）
        User trainer = new User();
        trainer.setId(userId);
        trainer.setEmail("trainer@example.com");
        trainer.setName("トレーナー");
        trainer.setKana("トレーナー");
        trainer.setRole(UserRole.TRAINER);

        // Lessonエンティティの作成（@Builderがないためnewで作成）
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setCustomer(customer);
        lesson.setStore(store);
        lesson.setTrainer(trainer);
        lesson.setStartDate(LocalDateTime.of(2025, 1, 10, 10, 0));
        lesson.setCreatedAt(LocalDateTime.now());

        // PostureImageエンティティの作成
        PostureImage image = PostureImage.builder()
                .id(imageId)
                .storageKey("/uploads/front.jpg")
                .consentPublication(true)
                .takenAt(OffsetDateTime.parse("2025-01-10T10:05:00Z"))
                .position(PostureImagePosition.FRONT)
                .createdAt(OffsetDateTime.parse("2025-01-10T10:06:00Z"))
                .build();

        // PostureGroupエンティティの作成（@Builderがないためnewで作成）
        PostureGroup group = new PostureGroup();
        group.setId(groupId);
        group.setCustomer(customer);
        group.setLesson(lesson);
        group.setCapturedAt(OffsetDateTime.parse("2025-01-10T10:07:00Z"));
        group.setCreatedAt(OffsetDateTime.parse("2025-01-10T10:08:00Z"));
        group.getImages().add(image);
        image.setPostureGroup(group);

        // モック設定: findByCustomerIdWithAuthを使用（Deprecatedメソッドは使用不可のため）
        when(postureGroupService.findByCustomerIdWithAuth(ArgumentMatchers.any(User.class), ArgumentMatchers.eq(customerId)))
                .thenReturn(List.of(PostureGroupResponse.fromEntity(group)));

        mockMvc.perform(get("/api/customers/{customerId}/posture_groups", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(groupId.toString()))
                .andExpect(jsonPath("$[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].images[0].id").value(imageId.toString()))
                .andExpect(jsonPath("$[0].images[0].position").value("front"));
    }

    @Test
    @DisplayName("姿勢画像の削除APIが204を返す")
    void deleteImageReturnsNoContent() throws Exception {
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/posture_images/{imageId}", imageId))
                .andExpect(status().isNoContent());
    }
}
