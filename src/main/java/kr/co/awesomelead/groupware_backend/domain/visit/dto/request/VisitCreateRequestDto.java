package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VisitCreateRequestDto {

    @NotBlank private String visitorName; // 내방객 이름

    @NotBlank
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    private String visitorPhone; // 내방객 전화번호 (하이픈 제외)

    @NotBlank private String visitorCompany; // 내방객 회사명

    private String carNumber; // 차량 번호

    @NotNull private VisitPurpose purpose; // 방문 목적

    @NotNull private LocalDateTime visitStartDate; // 방문 시작 일시

    @NotNull private Long hostUserId; // 담당자 ID

    @NotBlank private String hostCompany; // 담당자 회사명 (설치된 장소에 따라 클라이언트 단에서 자동 주입)

    @Valid private List<CompanionRequestDto> companions; // 동행자 목록

    @Size(min = 4, max = 4, message = "비밀번호는 4자리여야 합니다.")
    private String visitorPassword; // 내방객 비밀번호
}
