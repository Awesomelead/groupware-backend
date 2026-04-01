package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationMethod;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingCompletionStatus;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "안전보건 교육 세션 상세 응답")
public class SafetyTrainingSessionDetailResponseDto {

    @Schema(description = "세션 ID", example = "12")
    private Long sessionId;

    @Schema(description = "교육 제목", example = "2026년 1분기 정기 안전보건교육")
    private String title;

    @Schema(description = "교육 구분", example = "REGULAR")
    private SafetyEducationType educationType;

    @Schema(description = "교육 방법", example = "[\"LECTURE\", \"AUDIOVISUAL\"]")
    private List<SafetyEducationMethod> educationMethods;

    @Schema(description = "교육 시작 시각", example = "2026-03-24T08:30:00")
    private LocalDateTime startAt;

    @Schema(description = "교육 종료 시각", example = "2026-03-24T10:30:00")
    private LocalDateTime endAt;

    @Schema(description = "교육 장소", example = "3층 대회의실")
    private String place;

    @Schema(description = "회사 범위", example = "AWESOME")
    private Company companyScope;

    @Schema(description = "세션 상태", example = "OPEN")
    private SafetyTrainingSessionStatus status;

    @Schema(description = "교육 실시자 userId", example = "17")
    private Long instructorUserId;

    @Schema(description = "교육 실시자 이름", example = "고영민")
    private String instructorName;

    @Schema(description = "교육 대상 인원 수", example = "49")
    private int targetCount;

    @Schema(description = "교육 참석 인원 수", example = "40")
    private int attendedCount;

    @Schema(description = "교육 미참석 인원 수", example = "9")
    private int absentCount;

    @Schema(description = "교육 미참석 사유(세션 단위, 결석자 없으면 null)", example = "현장 장비 점검으로 일부 인원 교육 참여 불가")
    private String absentReasonSummary;

    @Schema(description = "보고서 파일 URL(확정본 생성 후)", example = "https://...presigned-url")
    private String reportFileUrl;

    @Schema(description = "내 참석 상태", example = "SIGNED")
    private SafetyTrainingAttendeeStatus myAttendanceStatus;

    @Schema(description = "내 수료 상태", example = "COMPLETED")
    private SafetyTrainingCompletionStatus myCompletionStatus;

    @Schema(description = "내 서명 시각", example = "2026-03-24T10:31:00")
    private LocalDateTime mySignedAt;

    @Schema(description = "내 서명 이미지 URL", example = "https://...signature.png")
    private String mySignatureUrl;

    @Schema(description = "내가 서명 가능한지 여부", example = "true")
    private boolean canSign;
}
