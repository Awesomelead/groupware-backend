package kr.co.awesomelead.groupware_backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@Schema(description = "개인정보 수정 요청 상세 응답 (현재 정보 vs 요청 정보 비교)")
public class AdminPendingMyInfoDetailResponseDto extends AdminUserDetailResponseDto {

    @Schema(description = "요청 ID", example = "101")
    private Long requestId;

    @Schema(description = "요청 상태", example = "PENDING")
    private MyInfoUpdateRequestStatus status;

    @Schema(description = "요청 생성 일시", example = "2026-02-26T15:20:00")
    private LocalDateTime requestedAt;

    @Schema(description = "요청 영문 이름", example = "HONG GILDONG NEW")
    private String requestedNameEng;

    @Schema(description = "요청 전화번호", example = "01099999999")
    private String requestedPhoneNumber;

    @Schema(description = "요청 우편번호", example = "06235")
    private String requestedZipcode;

    @Schema(description = "요청 주소1", example = "서울특별시 강남구 테헤란로 456")
    private String requestedAddress1;

    @Schema(description = "요청 주소2", example = "어썸리드빌딩 6층")
    private String requestedAddress2;
}
