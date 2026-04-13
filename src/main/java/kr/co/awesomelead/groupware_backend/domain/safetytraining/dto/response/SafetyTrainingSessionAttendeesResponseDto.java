package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingCompletionStatus;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "안전보건 교육일지 참석자 현황 응답")
public class SafetyTrainingSessionAttendeesResponseDto {

    @Schema(description = "교육일지 ID", example = "12")
    private Long sessionId;

    @Schema(description = "교육 대상 인원 수", example = "49")
    private int targetCount;

    @Schema(description = "교육 참석 인원 수", example = "40")
    private int attendedCount;

    @Schema(description = "교육 미참석 인원 수", example = "9")
    private int absentCount;

    @Schema(description = "교육 미참석 사유(교육일지 단위, 결석자 없으면 null)", example = "현장 장비 점검으로 일부 인원 교육 참여 불가")
    private String absentReasonSummary;

    @Schema(description = "참석자 목록")
    private List<AttendeeItem> attendees;

    @Getter
    @Builder
    public static class AttendeeItem {

        @Schema(description = "사용자 ID", example = "17")
        private Long userId;

        @Schema(description = "이름", example = "고영민")
        private String userName;

        @Schema(description = "부서명", example = "영업부")
        private String departmentName;

        @Schema(description = "참석 상태", example = "SIGNED")
        private SafetyTrainingAttendeeStatus attendanceStatus;

        @Schema(description = "수료 상태", example = "COMPLETED")
        private SafetyTrainingCompletionStatus completionStatus;

        @Schema(description = "서명 시각", example = "2026-03-24T10:31:00")
        private LocalDateTime signedAt;

        @Schema(description = "서명 이미지 URL", example = "https://...signature.png")
        private String signatureUrl;
    }
}
