package kr.co.awesomelead.groupware_backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "개인정보 수정 요청 대기 목록 응답")
public class MyInfoUpdateRequestSummaryResponseDto {

    @Schema(description = "요청 ID", example = "101")
    private Long requestId;

    @Schema(description = "요청자 사용자 ID", example = "17")
    private Long userId;

    @Schema(description = "요청자 한글 이름", example = "고영민")
    private String nameKor;

    @Schema(description = "요청자 이메일", example = "ko07073@gmail.com")
    private String email;

    @Schema(description = "요청 영문 이름", example = "KO YEONGMIN")
    private String requestedNameEng;

    @Schema(description = "요청 전화번호", example = "01012345678")
    private String requestedPhoneNumber;

    @Schema(description = "요청 우편번호", example = "06234")
    private String requestedZipcode;

    @Schema(description = "요청 주소1", example = "서울특별시 강남구 테헤란로 123")
    private String requestedAddress1;

    @Schema(description = "요청 주소2", example = "어썸리드빌딩 5층")
    private String requestedAddress2;

    @Schema(description = "요청 상태", example = "PENDING")
    private MyInfoUpdateRequestStatus status;

    @Schema(description = "요청 생성 일시", example = "2026-02-26T15:20:00")
    private LocalDateTime requestedAt;

    public static MyInfoUpdateRequestSummaryResponseDto from(MyInfoUpdateRequest request) {
        return MyInfoUpdateRequestSummaryResponseDto.builder()
                .requestId(request.getId())
                .userId(request.getUser().getId())
                .nameKor(request.getUser().getNameKor())
                .email(request.getUser().getEmail())
                .requestedNameEng(request.getRequestedNameEng())
                .requestedPhoneNumber(request.getRequestedPhoneNumber())
                .requestedZipcode(request.getRequestedZipcode())
                .requestedAddress1(request.getRequestedAddress1())
                .requestedAddress2(request.getRequestedAddress2())
                .status(request.getStatus())
                .requestedAt(request.getCreatedAt())
                .build();
    }
}
