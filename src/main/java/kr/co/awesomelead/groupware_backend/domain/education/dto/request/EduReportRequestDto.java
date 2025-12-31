package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.annotations.NotNull;

@Getter
@Builder
public class EduReportRequestDto {

    @NotNull
    private EduType eduType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private LocalDate eduDate;

    private boolean pinned;

    private boolean signatureRequired;

    private Long departmentId; // 부서교육인 경우에만 작성
}