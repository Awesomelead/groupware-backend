package kr.co.awesomelead.groupware_backend.domain.visit.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.PreVisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    // 사전 방문 예약
    @PostMapping("/pre-registration")
    public ResponseEntity<VisitResponseDto> createPreVisit(
        @RequestBody @Valid PreVisitCreateRequestDto requestDto) {

        VisitResponseDto responseDto = visitService.createPreVisit(requestDto);

        return ResponseEntity
            .created(URI.create("/api/visits/" + responseDto.getId()))
            .body(responseDto);
    }

    // 현장 방문 접수
    @Operation(summary = "현장 방문 접수", description = "내방객이 현장에서 방문접수를 수행합니다.")
    @PostMapping("/on-site")
    public ResponseEntity<VisitResponseDto> createOnSiteVisit(
        @RequestBody @Valid OnSiteVisitCreateRequestDto requestDto) {

        VisitResponseDto responseDto = visitService.createOnSiteVisit(requestDto);

        return ResponseEntity
            .created(URI.create("/api/visits/" + responseDto.getId()))
            .body(responseDto);
    }
}
