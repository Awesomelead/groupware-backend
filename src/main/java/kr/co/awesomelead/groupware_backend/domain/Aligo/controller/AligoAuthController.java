package kr.co.awesomelead.groupware_backend.domain.Aligo.controller;

import java.util.Map;
import kr.co.awesomelead.groupware_backend.domain.Aligo.service.AligoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aligo")
@RequiredArgsConstructor
public class AligoAuthController {

    private final AligoAuthService aligoAuthService;

    @GetMapping("/auth-request")
    public ResponseEntity<Map<String, Object>> authRequest() {
        Map<String, Object> result = aligoAuthService.requestChannelAuth();
        return ResponseEntity.ok(result);
    }
}