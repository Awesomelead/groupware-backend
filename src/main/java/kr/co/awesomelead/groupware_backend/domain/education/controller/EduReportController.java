package kr.co.awesomelead.groupware_backend.domain.education.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportAdminDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService.FileDownloadDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu-reports")
public class EduReportController {

    private final EduReportService eduReportService;
    private final EduAttachmentRepository eduAttachmentRepository;

    @Operation(summary = "교육 보고서 생성", description = "교육 보고서를 생성합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createEduReport(
        @RequestPart("requestDto") @Valid EduReportRequestDto requestDto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        @AuthenticationPrincipal CustomUserDetails userDetails)
        throws IOException {

        Long reportId = eduReportService.createEduReport(requestDto, files, userDetails.getId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(reportId)
            .toUri();
        
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "교육 보고서 목록 조회", description = "교육 보고서 목록을 조회합니다. 교육 유형으로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<List<EduReportSummaryDto>> getEduReports(
        @RequestParam(required = false) EduType type,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSummaryDto> reports =
            eduReportService.getEduReports(type, userDetails.getId());
        return ResponseEntity.ok(reports);
    }

    @Operation(summary = "교육 보고서 조회", description = "교육 보고서의 상세 정보를 조회합니다.")
    @GetMapping("/{eduReportId}")
    public ResponseEntity<EduReportDetailDto> getEduReport(
        @PathVariable Long eduReportId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        EduReportDetailDto report = eduReportService.getEduReport(eduReportId, userDetails.getId());
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "교육 보고서 삭제", description = "교육 보고서를 삭제합니다.")
    @DeleteMapping("/{eduReportId}")
    public ResponseEntity<Void> deleteEduReport(
        @PathVariable Long eduReportId, @AuthenticationPrincipal CustomUserDetails userDetails)
        throws IOException {
        eduReportService.deleteEduReport(eduReportId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "첨부파일 다운로드", description = "교육 보고서 첨부파일을 다운로드합니다.")
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) {
        FileDownloadDto downloadDto = eduReportService.getFileForDownload(id);

        String encodedFileName =
            UriUtils.encode(downloadDto.originalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(downloadDto.fileSize())
            .body(downloadDto.fileData());
    }

//    @GetMapping("/attachments/{id}/download")
//    public ResponseEntity<Void> downloadAttachment(@PathVariable Long id) {
//        String downloadUrl = eduReportService.getDownloadUrl(id);
//
//        return ResponseEntity.status(HttpStatus.FOUND)
//            .location(URI.create(downloadUrl))
//            .build();
//    }

    @Operation(summary = "출석 체크", description = "png 서명 이미지를 통해 교육 보고서에 대한 출석 체크를 수행합니다.")
    @PostMapping("/{id}/attendance")
    public ResponseEntity<String> markAttendance(
        @PathVariable Long id,
        @RequestPart(value = "signature", required = false) MultipartFile signature,
        @AuthenticationPrincipal CustomUserDetails userDetails)
        throws IOException {

        eduReportService.markAttendance(id, signature, userDetails.getId());

        return ResponseEntity.ok("출석 체크가 완료되었습니다.");
    }

    @Operation(summary = "관리자용 교육 보고서 상세 조회", description = "관리자 권한으로 교육 보고서의 상세 정보를 조회합니다.")
    @GetMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EduReportAdminDetailDto> getEduReportForAdmin(@PathVariable Long id) {

        EduReportAdminDetailDto response = eduReportService.getEduReportForAdmin(id);

        return ResponseEntity.ok(response);
    }
}
