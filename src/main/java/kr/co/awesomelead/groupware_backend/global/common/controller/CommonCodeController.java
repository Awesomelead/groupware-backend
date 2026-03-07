package kr.co.awesomelead.groupware_backend.global.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import kr.co.awesomelead.groupware_backend.global.common.dto.response.EnumCodeDto;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import kr.co.awesomelead.groupware_backend.global.common.service.CommonCodeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Common", description = "시스템 공통 코드 API")
@RestController
@RequestMapping("/api/common/codes")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    @Operation(
            summary = "부서 코드 목록 조회",
            description = "프론트엔드 드롭다운 등에 필요한 부서(DepartmentName) 코드 목록을 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                    {
                      "isSuccess": true,
                      "code": "COMMON200",
                      "message": "요청에 성공했습니다.",
                      "result": [
                        { "code": "CHUNGNAM_HQ", "description": "충남사업본부" },
                        { "code": "MARUI_LAB", "description": "(주)한국마루이 연구소" }
                      ]
                    }
                    """)))
            })
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<EnumCodeDto>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.onSuccess(commonCodeService.getDepartments()));
    }

    @Operation(summary = "직급 코드 목록 조회", description = "직급(Position) 코드 목록을 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                    {
                      "isSuccess": true,
                      "code": "COMMON200",
                      "message": "요청에 성공했습니다.",
                      "result": [
                        { "code": "CEO", "description": "대표이사" },
                        { "code": "STAFF", "description": "사원" }
                      ]
                    }
                    """)))
            })
    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<EnumCodeDto>>> getPositions() {
        return ResponseEntity.ok(ApiResponse.onSuccess(commonCodeService.getPositions()));
    }

    @Operation(summary = "근무 직종 코드 목록 조회", description = "근무 직종(JobType) 코드 목록을 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                    {
                      "isSuccess": true,
                      "code": "COMMON200",
                      "message": "요청에 성공했습니다.",
                      "result": [
                        { "code": "FIELD", "description": "현장직" },
                        { "code": "MANAGEMENT", "description": "관리직" }
                      ]
                    }
                    """)))
            })
    @GetMapping("/job-types")
    public ResponseEntity<ApiResponse<List<EnumCodeDto>>> getJobTypes() {
        return ResponseEntity.ok(ApiResponse.onSuccess(commonCodeService.getJobTypes()));
    }
}
