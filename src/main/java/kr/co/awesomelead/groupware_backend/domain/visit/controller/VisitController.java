package kr.co.awesomelead.groupware_backend.domain.visit.controller;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    // 사전 방문 예약
    @Operation(summary = "사전 방문 접수", description = "내방객이 온라인으로 사전방문접수를 수행합니다.")
    @PostMapping(value = "/pre-registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitResponseDto>> createPreVisit(
            @RequestPart("requestDto") @Valid VisitCreateRequestDto requestDto,
            @RequestPart(value = "signatureFile") MultipartFile signatureFile)
            throws IOException {

        VisitResponseDto responseDto = visitService.createPreVisit(requestDto, signatureFile);

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/visits/{id}")
                        .buildAndExpand(responseDto.getId())
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(responseDto));
    }

    // 현장 방문 접수
    @Operation(summary = "현장 방문 접수", description = "내방객이 현장에서 방문접수를 수행합니다.")
    @PostMapping(value = "/on-site", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitResponseDto>> createOnSiteVisit(
            @RequestPart("requestDto") @Valid VisitCreateRequestDto requestDto,
            @RequestPart("signatureFile") MultipartFile signatureFile)
            throws IOException {

        VisitResponseDto responseDto = visitService.createOnSiteVisit(requestDto, signatureFile);

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/visits/{id}")
                        .buildAndExpand(responseDto.getId())
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(responseDto));
    }

    @Operation(summary = "내 방문 정보 조회 ", description = "내방객이 사전등록 정보를 조회합니다.")
    @PostMapping("/visitor")
    public ResponseEntity<ApiResponse<List<VisitSummaryResponseDto>>> getMyVisits(
            @RequestBody @Valid VisitSearchRequestDto requestDto) {

        List<VisitSummaryResponseDto> responseDto = visitService.getMyVisits(requestDto);

        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "내 방문 상세 정보 조회", description = "내방객이 사전등록 정보에 대한 상세 정보를 조회합니다.")
    @GetMapping("/visitor/{visitId}")
    public ResponseEntity<ApiResponse<MyVisitResponseDto>> getMyVisitDetail(
            @PathVariable Long visitId) {
        MyVisitResponseDto responseDto = visitService.getMyVisitDetail(visitId);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "부서별 방문 정보 조회", description = "직원이 특정 부서 혹은 전체 내방객 정보 목록을 조회합니다.")
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<VisitSummaryResponseDto>>> getVisitsByDepartment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(required = false) Long departmentId) {

        List<VisitSummaryResponseDto> responseDtoList =
                visitService.getVisitsByDepartment(userDetails.getId(), departmentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(responseDtoList));
    }

    @Operation(summary = "직원용 방문 상세 정보 조회", description = "직원이 내방객 정보에 대한 상세 정보를 조회합니다.")
    @GetMapping("/employee/{visitId}")
    public ResponseEntity<ApiResponse<VisitDetailResponseDto>> getVisitDetailByEmployee(
            @PathVariable Long visitId) {

        VisitDetailResponseDto responseDto = visitService.getVisitDetailByEmployee(visitId);

        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "방문 처리", description = "내방객이 사전등록 정보에 대해 현장에서 방문처리를 수행합니다.")
    @PatchMapping("/{visitId}/check-in")
    public ResponseEntity<ApiResponse<Void>> checkIn(@PathVariable Long visitId) {
        visitService.checkIn(visitId);

        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Operation(summary = "퇴실 처리", description = "내방객의 방문에 대해 퇴실 처리를 수행합니다.")
    @PatchMapping("/check-out")
    public ResponseEntity<ApiResponse<Void>> checkOut(@RequestBody CheckOutRequestDto requestDto) {
        visitService.checkOut(requestDto);

        return ResponseEntity.ok(ApiResponse.onNoContent());
    }
}
