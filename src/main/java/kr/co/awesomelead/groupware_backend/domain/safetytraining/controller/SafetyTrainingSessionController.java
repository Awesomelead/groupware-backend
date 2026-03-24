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
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingPreviewResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.service.SafetyTrainingSessionService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/safety-trainings")
@Tag(
        name = "Safety Training",
        description =
                """
        안전보건 교육 세션 API

        ### 권한
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

    @Operation(summary = "안전보건 교육 엑셀 미리보기", description = "입력한 값으로 DB 저장 없이 엑셀 미리보기를 생성합니다.")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<SafetyTrainingPreviewResponseDto>> preview(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SafetyTrainingSessionCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(safetyTrainingSessionService.preview(userDetails.getId(), requestDto)));
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
                                                                    SafetyTrainingSessionCreateRequestDto.class),
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
