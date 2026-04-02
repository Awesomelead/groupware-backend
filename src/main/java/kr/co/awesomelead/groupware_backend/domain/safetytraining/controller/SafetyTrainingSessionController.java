package kr.co.awesomelead.groupware_backend.domain.safetytraining.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionStatusUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingPreviewResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionAttendeesResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionReportResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.service.SafetyTrainingSessionService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/safety-trainings")
@Tag(
        name = "Safety Training",
        description =
                """
        안전보건 교육 세션 API

        ### 권한
        - 조회: 일반 사용자(`본인 회사만 조회`)
        - 조회: `WRITE_SAFETY` 권한 사용자(`전체 회사 조회 가능`)
        - 참석자 현황 조회: `WRITE_SAFETY`
        - 세션 상태 변경/미참석 사유 입력: `WRITE_SAFETY`
        - 세션 삭제: `WRITE_SAFETY`
        - 보고서 생성(엑셀): `WRITE_SAFETY` (`OPEN/CLOSED` 모두 가능)
        - 보고서 다운로드 URL 조회: 세션 조회 권한 사용자
        - 수료 처리(서명): 세션이 `OPEN`이고 미수료(`PENDING`, `ABSENT`) 상태일 때 본인 서명 가능
        - 생성: `WRITE_SAFETY`

        ### 사용 Enum
        - `SafetyEducationType`
          - `REGULAR`(정기교육), `HIRING`(채용시), `JOB_CHANGE`(작업내용 변경시), `SPECIAL`(특별교육), `MSDS`(MSDS교육)
        - `SafetyEducationMethod`
          - `LECTURE`(강의), `AUDIOVISUAL`(시청각), `FIELD_TRAINING`(현장 교육), `DEMONSTRATION`(시범 실습), `TOUR`(견학), `ROLE_PLAY`(역할연기)
        - `Company`
          - `AWESOME`(어썸리드), `MARUI`(마루이)
        """)
public class SafetyTrainingSessionController {

    private final SafetyTrainingSessionService safetyTrainingSessionService;

    @Operation(
            summary = "안전보건 교육 세션 목록 조회",
            description =
                    "일반 사용자는 본인 회사 데이터를 조회할 수 있으며, WRITE_SAFETY 권한 사용자는" + " 전체 회사/상태 조회가 가능합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "USER_NOT_FOUND",
              "message": "해당 사용자를 찾을 수 없습니다.",
              "result": null
            }
            """)))
            })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SafetyTrainingSessionSummaryResponseDto>>> getSessions(
            @ParameterObject SafetyTrainingSessionSearchConditionDto condition,
            @ParameterObject
                    @PageableDefault(
                            page = 0,
                            size = 20,
                            sort = "createdAt",
                            direction = Sort.Direction.DESC)
                    Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<SafetyTrainingSessionSummaryResponseDto> result =
                safetyTrainingSessionService.getSessions(userDetails.getId(), condition, pageable);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "안전보건 교육 세션 상세 조회",
            description = "제목, 교육 정보, 보고서 파일 URL, 내 수료/미수료 및 서명 가능 여부를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "조회 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_READ",
              "message": "해당 안전보건 교육 조회 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션 또는 사용자 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "SAFETY_TRAINING_SESSION_NOT_FOUND",
                  "message": "해당 안전보건 교육 세션을 찾을 수 없습니다.",
                  "result": null
                }
                """)
                                        }))
            })
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<SafetyTrainingSessionDetailResponseDto>> getSessionDetail(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        SafetyTrainingSessionDetailResponseDto result =
                safetyTrainingSessionService.getSessionDetail(sessionId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "안전보건 교육 참석자 현황 조회",
            description = "작성 권한(WRITE_SAFETY) 사용자가 세션의 참석자 상태(PENDING/SIGNED/ABSENT)를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "조회 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
              "message": "PSM/안전보건 작성 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션/사용자 없음")
            })
    @GetMapping("/{sessionId}/attendees")
    public ResponseEntity<ApiResponse<SafetyTrainingSessionAttendeesResponseDto>>
            getSessionAttendees(
                    @PathVariable Long sessionId,
                    @Parameter(hidden = true) @AuthenticationPrincipal
                            CustomUserDetails userDetails) {
        SafetyTrainingSessionAttendeesResponseDto result =
                safetyTrainingSessionService.getSessionAttendees(sessionId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "안전보건 교육 보고서 생성",
            description =
                    "작성 권한(WRITE_SAFETY) 사용자가 세션 엑셀 보고서를 생성/재생성합니다. "
                            + "세션 상태(OPEN/CLOSED)와 무관하게 생성 가능합니다. "
                            + "서명 완료자의 서명 이미지는 참석자 이름 옆 칸에 반영됩니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "생성 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "작성 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
              "message": "PSM/안전보건 작성 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션/사용자 없음")
            })
    @PostMapping("/{sessionId}/report")
    public ResponseEntity<ApiResponse<SafetyTrainingSessionReportResponseDto>>
            generateSessionReport(
                    @PathVariable Long sessionId,
                    @Parameter(hidden = true) @AuthenticationPrincipal
                            CustomUserDetails userDetails) {
        SafetyTrainingSessionReportResponseDto result =
                safetyTrainingSessionService.generateSessionReport(sessionId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "안전보건 교육 보고서 다운로드 URL 조회",
            description = "세션 조회 권한이 있는 사용자가 보고서 다운로드 URL을 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "조회 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_READ",
              "message": "해당 안전보건 교육 조회 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션 없음 또는 보고서 미생성",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "SAFETY_TRAINING_REPORT_NOT_FOUND",
              "message": "안전보건 교육 보고서가 아직 생성되지 않았습니다.",
              "result": null
            }
            """)))
            })
    @GetMapping("/{sessionId}/report")
    public ResponseEntity<ApiResponse<SafetyTrainingSessionReportResponseDto>> getSessionReport(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        SafetyTrainingSessionReportResponseDto result =
                safetyTrainingSessionService.getSessionReport(sessionId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "안전보건 교육 세션 수정",
            description = "작성 권한(WRITE_SAFETY) 사용자가 세션을 수정합니다. OPEN 상태 + 서명 완료자 0명인 경우에만 수정 가능합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            examples =
                                                    @ExampleObject(
                                                            name = "수정 요청 예시",
                                                            value =
                                                                    """
            {
              "title": "2026년 1분기 정기 안전보건교육(수정)",
              "educationType": "REGULAR",
              "educationMethods": ["LECTURE", "AUDIOVISUAL"],
              "startAt": "2026-03-24T08:30:00",
              "endAt": "2026-03-24T10:30:00",
              "educationContent": "개인정보 보호 및 사내 보안 규정 안내",
              "place": "3층 대회의실",
              "instructorUserId": 17,
              "companyScope": "AWESOME"
            }
            """))))
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "수정 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "마감 세션 또는 서명 완료자 존재",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "마감 세션",
                                                    value =
                                                            """
            {
              "isSuccess": false,
              "code": "SAFETY_TRAINING_SESSION_CLOSED",
              "message": "마감된 안전보건 교육입니다.",
              "result": null
            }
            """),
                                            @ExampleObject(
                                                    name = "서명 완료자 존재",
                                                    value =
                                                            """
            {
              "isSuccess": false,
              "code": "SAFETY_TRAINING_SESSION_HAS_SIGNED_ATTENDEE",
              "message": "서명 완료자가 존재하여 교육을 수정할 수 없습니다.",
              "result": null
            }
            """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "수정 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
              "message": "PSM/안전보건 작성 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션/사용자 없음")
            })
    @PatchMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Long>> update(
            @PathVariable Long sessionId,
            @Valid @RequestBody SafetyTrainingSessionUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                safetyTrainingSessionService.update(sessionId, userDetails.getId(), requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            summary = "안전보건 교육 세션 삭제",
            description =
                    "작성 권한(WRITE_SAFETY) 사용자가 세션을 삭제합니다. "
                            + "세션 본문, 참석자 데이터, 생성된 보고서 파일, 참석자 서명 파일을 함께 정리합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "삭제 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "삭제 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
              "message": "PSM/안전보건 작성 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션/사용자 없음")
            })
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        safetyTrainingSessionService.delete(sessionId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Operation(
            summary = "안전보건 교육 세션 상태 변경",
            description =
                    "작성 권한(WRITE_SAFETY) 사용자가 세션 상태를 OPEN/CLOSED로 변경합니다. "
                            + "CLOSED(정상 마감) 전환 시 미서명(PENDING) 대상자는 자동으로 불참(ABSENT) 처리됩니다. "
                            + "이때 결석자가 존재하면 absentReasonSummary를 입력해야 합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            examples = {
                                                @ExampleObject(
                                                        name = "CLOSED 전환(결석자 있음)",
                                                        value =
                                                                """
            {
              "status": "CLOSED",
              "absentReasonSummary": "현장 장비 점검으로 일부 인원 교육 참여 불가"
            }
            """),
                                                @ExampleObject(
                                                        name = "CLOSED 전환(결석자 없음)",
                                                        value =
                                                                """
            {
              "status": "CLOSED",
              "absentReasonSummary": null
            }
            """),
                                                @ExampleObject(
                                                        name = "OPEN 전환",
                                                        value =
                                                                """
            {
              "status": "OPEN",
              "absentReasonSummary": null
            }
            """)
                                            })))
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "상태 변경 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "CLOSED 전환 + 결석자 존재 시 미참석 사유 누락",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "SAFETY_TRAINING_ABSENT_REASON_REQUIRED",
              "message": "결석자가 있을 경우 교육 미참석 사유 입력은 필수입니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "수정 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
              "message": "PSM/안전보건 작성 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션/사용자 없음")
            })
    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<ApiResponse<Long>> updateStatus(
            @PathVariable Long sessionId,
            @Valid @RequestBody SafetyTrainingSessionStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                safetyTrainingSessionService.updateStatus(
                        sessionId, userDetails.getId(), requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            summary = "안전보건 교육 수료 서명",
            description =
                    "본인의 미수료 상태(PENDING, ABSENT)를 PNG 서명 업로드 후 수료(SIGNED)로 변경합니다. OPEN 상태 세션에서만 서명할"
                            + " 수 있습니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "서명 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "이미 서명됨 / 서명 파일 누락 / 서명 형식 오류",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "이미 서명됨",
                                                    value =
                                                            """
            {
              "isSuccess": false,
              "code": "ALREADY_MARKED_ATTENDANCE",
              "message": "이미 출석이 체크된 교육입니다.",
              "result": null
            }
            """),
                                            @ExampleObject(
                                                    name = "서명 누락",
                                                    value =
                                                            """
            {
              "isSuccess": false,
              "code": "NO_SIGNATURE_PROVIDED",
              "message": "서명이 제공되지 않았습니다.",
              "result": null
            }
            """),
                                            @ExampleObject(
                                                    name = "서명 형식 오류",
                                                    value =
                                                            """
            {
              "isSuccess": false,
              "code": "INVALID_SIGNATURE_FORMAT",
              "message": "서명은 PNG 파일 형식만 지원합니다.",
              "result": null
            }
            """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "서명 권한 없음(타 회사)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_READ",
              "message": "해당 안전보건 교육 조회 권한이 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "세션/대상자 없음")
            })
    @PostMapping(
            value = "/{sessionId}/attendance",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> signAttendance(
            @PathVariable Long sessionId,
            @RequestPart(value = "signature", required = false) MultipartFile signature,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {
        safetyTrainingSessionService.signAttendance(sessionId, userDetails.getId(), signature);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(summary = "안전보건 교육 엑셀 미리보기", description = "입력한 값으로 DB 저장 없이 엑셀 미리보기를 생성합니다.")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<SafetyTrainingPreviewResponseDto>> preview(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SafetyTrainingSessionCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        safetyTrainingSessionService.preview(userDetails.getId(), requestDto)));
    }

    @Operation(
            summary = "안전보건 교육 세션 생성",
            description = "PSM/안전보건 작성 권한 사용자가 교육 세션을 생성합니다.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    SafetyTrainingSessionCreateRequestDto
                                                                            .class),
                                            examples =
                                                    @ExampleObject(
                                                            value =
                                                                    """
                    {
                      "title": "2026년 1분기 정기 안전보건교육",
                      "educationType": "REGULAR",
                      "educationMethods": ["LECTURE", "AUDIOVISUAL"],
                      "startAt": "2026-03-24T08:30:00",
                      "endAt": "2026-03-24T10:30:00",
                      "educationContent": "개인정보 보호 및 사내 보안 규정 안내",
                      "place": "3층 대회의실",
                      "instructorUserId": 17,
                      "companyScope": "AWESOME"
                    }
                    """))))
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "생성 성공",
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
              "code": "COMMON200",
              "message": "요청에 성공했습니다.",
              "result": 1
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "INVALID_TIME_RANGE",
              "message": "퇴실 예정 시간은 입실 예정 시간보다 빠를 수 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "USER_NOT_FOUND",
              "message": "해당 사용자를 찾을 수 없습니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
              "message": "PSM/안전보건 작성 권한이 없습니다.",
              "result": null
            }
            """)))
            })
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SafetyTrainingSessionCreateRequestDto requestDto) {

        Long sessionId = safetyTrainingSessionService.create(userDetails.getId(), requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(sessionId));
    }
}
