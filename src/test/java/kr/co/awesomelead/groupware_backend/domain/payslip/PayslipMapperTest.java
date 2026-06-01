package kr.co.awesomelead.groupware_backend.domain.payslip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import kr.co.awesomelead.groupware_backend.domain.payslip.mapper.PayslipMapper;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.text.Normalizer;
import java.time.LocalDateTime;

class PayslipMapperTest {

    private final PayslipMapper payslipMapper = Mappers.getMapper(PayslipMapper.class);

    @Test
    void toAdminPayslipSummaryDto_shouldMapNameAndOriginalFileNameWithoutTitleConversion() {
        String originalFileName = "급여명세서(근로기준1)_10002_리딘뷰_202604.pdf";
        User user = User.builder().nameKor("리딘뷰").position(Position.STAFF).build();
        Payslip payslip =
                Payslip.builder()
                        .id(5L)
                        .user(user)
                        .originalFileName(originalFileName)
                        .createdAt(LocalDateTime.of(2026, 5, 31, 15, 29, 4))
                        .status(PayslipStatus.SENT)
                        .build();

        AdminPayslipSummaryDto response = payslipMapper.toAdminPayslipSummaryDto(payslip);

        assertThat(response.getEmployeeName()).isEqualTo("리딘뷰");
        assertThat(response.getEmployPosition()).isEqualTo("사원");
        assertThat(response.getOriginalFileName()).isEqualTo(originalFileName);
        assertThat(response.getPayslipTitle()).isEqualTo("2026년 4월");
    }

    @Test
    void toAdminPayslipSummaryDto_shouldBuildTitleForNfdFileName() {
        String nfdFileName =
                Normalizer.normalize("급여명세서(근로기준1)_10002_리딘뷰_202604.pdf", Normalizer.Form.NFD);
        User user = User.builder().nameKor("리딘뷰").position(Position.STAFF).build();
        Payslip payslip =
                Payslip.builder()
                        .id(6L)
                        .user(user)
                        .originalFileName(nfdFileName)
                        .createdAt(LocalDateTime.of(2026, 5, 31, 15, 29, 4))
                        .status(PayslipStatus.SENT)
                        .build();

        AdminPayslipSummaryDto response = payslipMapper.toAdminPayslipSummaryDto(payslip);

        assertThat(response.getOriginalFileName()).isEqualTo(nfdFileName);
        assertThat(response.getPayslipTitle()).isEqualTo("2026년 4월");
    }

    @Test
    void toAdminPayslipDetailDto_shouldMapPositionAsKorean() {
        String originalFileName = "급여명세서(근로기준1)_10002_리딘뷰_202604.pdf";
        User user = User.builder().nameKor("리딘뷰").position(Position.ASSISTANT_MANAGER).build();
        Payslip payslip =
                Payslip.builder()
                        .id(7L)
                        .user(user)
                        .fileKey("payslip-7")
                        .originalFileName(originalFileName)
                        .createdAt(LocalDateTime.of(2026, 5, 31, 15, 29, 4))
                        .status(PayslipStatus.SENT)
                        .build();

        S3Service s3Service = mock(S3Service.class);
        when(s3Service.getPresignedViewUrl("payslip-7"))
                .thenReturn("https://example.com/payslip-7");

        AdminPayslipDetailDto response = payslipMapper.toAdminPayslipDetailDto(payslip, s3Service);

        assertThat(response.getEmployPosition()).isEqualTo("대리");
    }

    @Test
    void buildPayslipTitle_throwsWhenPayslipIsNull() {
        assertThatThrownBy(() -> payslipMapper.buildPayslipTitle(null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYSLIP_NOT_FOUND);
    }

    @Test
    void buildPayslipTitle_throwsWhenOriginalFileNameIsNull() {
        Payslip payslip = Payslip.builder().id(1L).originalFileName(null).build();

        assertThatThrownBy(() -> payslipMapper.toAdminPayslipSummaryDto(payslip))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
    }

    @Test
    void buildPayslipTitle_throwsWhenOriginalFileNameIsInvalidFormat() {
        Payslip payslip = Payslip.builder().id(1L).originalFileName("잘못된_파일명.pdf").build();

        assertThatThrownBy(() -> payslipMapper.toAdminPayslipSummaryDto(payslip))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
    }
}
