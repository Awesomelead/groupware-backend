package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EducationCategoryReorderRequestDto {

    @Schema(description = "같은 부모를 가진 카테고리 ID의 정렬 순서 목록", example = "[1,2,3,4,5]")
    @NotEmpty(message = "정렬할 카테고리 ID 목록은 필수입니다.")
    private List<Long> categoryIds;
}

