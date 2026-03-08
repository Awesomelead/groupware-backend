package kr.co.awesomelead.groupware_backend.domain.requesthistory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.request.RequestHistoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistoryDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.service.RequestHistoryService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/request-histories")
@Tag(name = "RequestHistory", description = "사용자 제증명 발급 신청 API")
public class RequestHistoryController {

    private final RequestHistoryService requestHistoryService;

    @Operation(summary = "제증명 발급 신청", description = "현재 로그인한 사용자가 제증명 발급을 신청합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "신청 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON201",
                                  "message": "성공적으로 생성되었습니다.",
                                  "result": 1
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "입력값 오류",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON400",
                                  "message": "입력값이 유효하지 않습니다.",
                                  "result": { "purpose": "용도는 필수입니다." }
                                }
                                """)))
            })
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestHistoryCreateRequestDto requestDto) {
        Long requestId = requestHistoryService.createRequest(userDetails.getId(), requestDto);
        return ResponseEntity.status(201).body(ApiResponse.onCreated(requestId));
    }

    @Operation(summary = "내 제증명 발급 신청 목록 조회", description = "현재 로그인한 사용자의 신청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RequestHistorySummaryResponseDto>>> getMyRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RequestHistorySummaryResponseDto> result =
                requestHistoryService.getMyRequests(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "내 제증명 발급 신청 상세 조회", description = "현재 로그인한 사용자의 신청 상세를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "신청 내역 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "REQUEST_HISTORY_NOT_FOUND",
                                  "message": "해당 제증명 발급 신청 내역을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<RequestHistoryDetailResponseDto>> getMyRequestDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "신청 ID", example = "1", required = true) @PathVariable
                    Long requestId) {
        RequestHistoryDetailResponseDto result =
                requestHistoryService.getMyRequestDetail(userDetails.getId(), requestId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "내 제증명 발급 신청 취소", description = "현재 로그인한 사용자가 본인의 대기 상태 신청을 취소합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "취소 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "취소 불가 상태",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "REQUEST_HISTORY_NOT_CANCELABLE",
                                  "message": "대기 상태 요청만 취소할 수 있습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelMyRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "취소할 신청 ID", example = "1", required = true) @PathVariable
                    Long requestId) {
        requestHistoryService.cancelMyRequest(userDetails.getId(), requestId);
        return ResponseEntity.ok(ApiResponse.onSuccess("신청이 취소되었습니다."));
    }
}
