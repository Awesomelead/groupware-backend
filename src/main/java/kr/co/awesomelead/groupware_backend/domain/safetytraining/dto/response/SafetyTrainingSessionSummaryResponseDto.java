package kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "안전보건 교육 세션 목록 항목")
public class SafetyTrainingSessionSummaryResponseDto {

    @Schema(description = "세션 ID", example = "12")
    private Long sessionId;

    @Schema(description = "교육 제목", example = "2026년 1분기 정기 안전보건교육")
    private String title;

    @Schema(description = "교육 구분", example = "REGULAR")
    private SafetyEducationType educationType;

    @Schema(description = "교육 시작 시각", example = "2026-03-24T08:30:00")
    private LocalDateTime startAt;

    @Schema(description = "교육 종료 시각", example = "2026-03-24T10:30:00")
    private LocalDateTime endAt;

    @Schema(description = "교육 장소", example = "3층 대회의실")
    private String place;

    @Schema(description = "회사 범위", example = "AWESOME")
    private Company companyScope;

    @Schema(description = "교육 실시자 userId", example = "17")
    private Long instructorUserId;

    @Schema(description = "교육 실시자 이름(스냅샷)", example = "고영민")
    private String instructorName;

    @Schema(description = "세션 상태", example = "OPEN")
    private SafetyTrainingSessionStatus status;

    @Schema(description = "대상 인원 수", example = "49")
    private int targetCount;

    @Schema(description = "참석 인원 수", example = "40")
    private int attendedCount;

    @Schema(description = "미참석 인원 수", example = "9")
    private int absentCount;

    @Schema(description = "현재 로그인 사용자의 서명 완료 여부", example = "true")
    private boolean mySigned;

    @Schema(description = "생성 시각", example = "2026-03-24T11:00:00")
    private LocalDateTime createdAt;
}
