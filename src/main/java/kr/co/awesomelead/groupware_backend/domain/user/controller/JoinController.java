package kr.co.awesomelead.groupware_backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kr.co.awesomelead.groupware_backend.domain.user.dto.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.service.JoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JoinController {

    private final JoinService joinService;

    @Operation(summary="회원가입", description="회원가입을 요청합니다.")
    @PostMapping("/join")
    public ResponseEntity<String> joinProcess(@Valid @RequestBody JoinRequestDto joinDto) {
        joinService.joinProcess(joinDto);
        return ResponseEntity.ok("회원가입 요청이 성공적으로 완료되었습니다. 관리자 승인을 기다려주세요.");
    }
}
