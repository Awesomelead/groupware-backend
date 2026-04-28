package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
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

    @Schema(description = "승인자 타겟 설정")
    private ApprovalTargetResponseDto approvers;

    @Schema(description = "열람권자 타겟 설정(결재 완료 후 열람 가능)")
    private ApprovalTargetResponseDto viewers;

    @Schema(description = "참조자 타겟 설정(결재 진행 전 과정에서 열람 가능)")
    private ApprovalTargetResponseDto referrers;

    public static ApprovalConfigResponseDto from(
            ApprovalLineConfig config,
            Map<Long, User> usersById,
            Map<Long, Department> departmentsById) {
        return ApprovalConfigResponseDto.builder()
                .documentType(config.getDocumentType().name())
                .documentTypeLabel(config.getDocumentType().getDescription())
                .approvers(
                        toTargetDto(
                                config.getApproverTargetDepartmentIds(),
                                config.getApproverTargetUserIds(),
                                usersById,
                                departmentsById))
                .viewers(
                        toTargetDto(
                                config.getViewerTargetDepartmentIds(),
                                config.getViewerTargetUserIds(),
                                usersById,
                                departmentsById))
                .referrers(
                        toTargetDto(
                                config.getReferrerTargetDepartmentIds(),
                                config.getReferrerTargetUserIds(),
                                usersById,
                                departmentsById))
                .build();
    }

    private static ApprovalTargetResponseDto toTargetDto(
            List<Long> departmentIds,
            List<Long> userIds,
            Map<Long, User> usersById,
            Map<Long, Department> departmentsById) {
        List<Long> safeDepartmentIds =
                departmentIds == null ? Collections.emptyList() : departmentIds;
        List<Long> safeUserIds = userIds == null ? Collections.emptyList() : userIds;

        List<ApprovalLineDepartmentDto> departments =
                safeDepartmentIds.stream()
                        .map(departmentsById::get)
                        .filter(Objects::nonNull)
                        .map(
                                department ->
                                        ApprovalLineDepartmentDto.builder()
                                                .id(department.getId())
                                                .name(
                                                        department.getName() == null
                                                                ? null
                                                                : department
                                                                        .getName()
                                                                        .getDescription())
                                                .company(
                                                        department.getCompany() == null
                                                                ? null
                                                                : department
                                                                        .getCompany()
                                                                        .getDescription())
                                                .build())
                        .toList();

        List<ApprovalLineUserDto> users =
                safeUserIds.stream()
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
                                                                : user.getPosition()
                                                                        .getDescription())
                                                .departmentName(
                                                        user.getDepartment() == null
                                                                        || user.getDepartment()
                                                                                        .getName()
                                                                                == null
                                                                ? null
                                                                : user.getDepartment()
                                                                        .getName()
                                                                        .getDescription())
                                                .build())
                        .toList();

        return ApprovalTargetResponseDto.builder()
                .targetDepartment(departments)
                .targetUser(users)
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결재선 역할별 타겟 응답")
    public static class ApprovalTargetResponseDto {
        @Schema(description = "대상 부서 상세 목록")
        private List<ApprovalLineDepartmentDto> targetDepartment;

        @Schema(description = "대상 사용자 상세 목록")
        private List<ApprovalLineUserDto> targetUser;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결재선 사용자 상세 정보")
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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결재선 부서 상세 정보")
    public static class ApprovalLineDepartmentDto {
        @Schema(description = "부서 ID", example = "1")
        private Long id;

        @Schema(description = "부서명", example = "경영지원부")
        private String name;

        @Schema(description = "회사명", example = "어썸리드")
        private String company;
    }
}
