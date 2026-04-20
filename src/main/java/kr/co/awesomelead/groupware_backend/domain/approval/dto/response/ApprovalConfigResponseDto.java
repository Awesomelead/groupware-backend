package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결재선 설정 응답 DTO")
public class ApprovalConfigResponseDto {

    @Schema(description = "문서 양식 타입(영문 enum)", example = "BASIC")
    private String documentType;

    @Schema(description = "문서 양식 한글 라벨", example = "기본양식")
    private String documentTypeLabel;

    @Schema(description = "결재자 상세 정보 목록 (순서 보장)")
    private List<ApprovalLineUserDto> approvers;

    @Schema(description = "참조자 상세 정보 목록")
    private List<ApprovalLineUserDto> referrers;

    public static ApprovalConfigResponseDto from(
            ApprovalLineConfig config, Map<Long, User> usersById) {
        List<Long> approverIds =
                config.getApproverIds() == null ? Collections.emptyList() : config.getApproverIds();
        List<Long> referrerIds =
                config.getReferrerIds() == null ? Collections.emptyList() : config.getReferrerIds();

        return ApprovalConfigResponseDto.builder()
                .documentType(config.getDocumentType().name())
                .documentTypeLabel(config.getDocumentType().getDescription())
                .approvers(toUserDtos(approverIds, usersById))
                .referrers(toUserDtos(referrerIds, usersById))
                .build();
    }

    private static List<ApprovalLineUserDto> toUserDtos(List<Long> ids, Map<Long, User> usersById) {
        return ids.stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(
                        user ->
                                ApprovalLineUserDto.builder()
                                        .id(user.getId())
                                        .name(user.getDisplayName())
                                        .position(
                                                user.getPosition() == null
                                                        ? null
                                                        : user.getPosition().getDescription())
                                        .departmentName(
                                                user.getDepartment() == null
                                                                || user.getDepartment().getName()
                                                                        == null
                                                        ? null
                                                        : user.getDepartment()
                                                                .getName()
                                                                .getDescription())
                                        .build())
                .toList();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결재선 유저 상세 정보")
    public static class ApprovalLineUserDto {
        @Schema(description = "사용자 ID", example = "14")
        private Long id;

        @Schema(description = "이름", example = "고영민")
        private String name;

        @Schema(description = "직급", example = "사원")
        private String position;

        @Schema(description = "부서명", example = "경영지원부")
        private String departmentName;
    }
}
