package kr.co.awesomelead.groupware_backend.domain.visit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckInRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitDetailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visits")
@Tag(
    name = "Visit",
    description =
        """
            ## 방문 관리 API

            내방객의 사전 예약 및 현장 방문 접수, 방문 정보 조회, 입/퇴실 처리 등을 수행합니다.

            ### 사용되는 Enum 타입
            - **VisitType**: 방문 유형 (PRE_REGISTRATION: 사전 예약, ON_SITE: 현장 방문)
            - **VisitPurpose**: 방문 목적 (CUSTOMER_INSPECTION: 고객 검수, GOODS_DELIVERY: 물품 납품, FACILITY_CONSTRUCTION: 시설공사, LOGISTICS: 입출고, MEETING: 미팅, OTHER: 기타)
            """)
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "사전 하루 방문 신청", description = "방문 전 내방객이 하루 방문을 사전에 신청합니다.")
    @PostMapping("/pre-registration/one-day")
    public ResponseEntity<ApiResponse<Long>> registerOneDayPreVisit(
        @Valid @RequestBody OneDayVisitRequestDto dto) {

        Long visitId = visitService.registerOneDayPreVisit(dto);
        return ResponseEntity.ok(ApiResponse.onSuccess(visitId));
    }

    @Operation(summary = "사전 장기 방문 신청", description = "방문 전 내방객이 장기 방문을 사전에 신청합니다.")
    @PostMapping("/pre-registration/long-term")
    public ResponseEntity<ApiResponse<Long>> registerLongTermPreVisit(
        @Valid @RequestBody LongTermVisitRequestDto dto) {

        Long visitId = visitService.registerLongTermPreVisit(dto);
        return ResponseEntity.ok(ApiResponse.onSuccess(visitId));
    }

    @Operation(summary = "현장 방문 신청 및 즉시 입실", description = "안내 데스크에서 현장 방문객 정보를 입력하고 서명을 받아 즉시 입실 처리합니다.")
    @PostMapping(value = "/on-site", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createOnSiteVisit(
        @Valid @ModelAttribute OnSiteVisitRequestDto requestDto) throws IOException {

        Long visitId = visitService.registerOnSiteVisit(requestDto);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/visits/{id}")
            .buildAndExpand(visitId)
            .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(visitId));
    }

    @Operation(summary = "사전 예약자 입실 처리", description = "사전 신청한 내방객이 도착했을 때 서명을 받고 입실을 완료합니다.")
    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> checkIn(
        @Valid @ModelAttribute CheckInRequestDto requestDto) throws IOException {

        Long visitId = visitService.checkIn(requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(visitId));
    }

    @Operation(summary = "방문자 퇴실 처리", description = "입실 중인 내방객의 퇴실 시간을 기록하고 방문 상태를 업데이트합니다.")
    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<Long>> checkOut(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CheckOutRequestDto dto) {

        Long response = visitService.checkOut(userDetails.getId(), dto);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
        summary = "내 방문 목록 조회",
        description = "성함, 전화번호, 비밀번호를 입력하여 신청한 방문 내역 목록을 확인합니다."
    )
    @PostMapping("/my")
    public ResponseEntity<ApiResponse<List<MyVisitListResponseDto>>> getMyVisitList(
        @Valid @RequestBody VisitSearchRequestDto dto) {

        List<MyVisitListResponseDto> response = visitService.getMyVisitList(dto);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
        summary = "내 방문 상세 조회",
        description = "방문 ID와 목록 조회 시 사용한 비밀번호를 통해 상세 정보를 확인합니다."
    )
    @PostMapping("/{visitId}/detail")
    public ResponseEntity<ApiResponse<MyVisitDetailResponseDto>> getMyVisitDetail(
        @PathVariable Long visitId,
        @Valid @RequestBody MyVisitDetailRequestDto dto) {

        MyVisitDetailResponseDto response = visitService.getMyVisitDetail(visitId, dto);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "직원용 내방객 목록 조회", description = "관리 직군이 전체 내방객 목록을 조회합니다. 부서 ID로 필터링이 가능합니다.")
    @GetMapping("/admin/list")
    public ResponseEntity<ApiResponse<List<VisitListResponseDto>>> getAdminVisitList(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) Long departmentId) {

        List<VisitListResponseDto> response = visitService.getVisitsForAdmin(userDetails.getId(),
            departmentId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
        summary = "직원용 내방객 상세 조회",
        description = "관리 직군이 특정 내방객의 상세 정보, 서명 이미지 및 모든 입퇴실 기록을 조회합니다."
    )
    @GetMapping("/admin/{visitId}")
    public ResponseEntity<ApiResponse<VisitDetailResponseDto>> getAdminVisitDetail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long visitId) {

        VisitDetailResponseDto response = visitService.getVisitDetailForAdmin(userDetails.getId(),
            visitId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "장기 방문 신청 승인", description = "관리 직군이 PENDING 상태인 장기 방문 신청을 승인합니다.")
    @PostMapping("/admin/{visitId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveVisit(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long visitId) {

        visitService.approveVisit(userDetails.getId(), visitId);
        return ResponseEntity.ok(ApiResponse.onNoContent("방문 신청 승인이 완료되었습니다."));
    }
}
