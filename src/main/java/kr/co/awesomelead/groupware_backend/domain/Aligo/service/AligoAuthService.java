package kr.co.awesomelead.groupware_backend.domain.Aligo.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AligoAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${aligo.api.key}")
    private String apiKey;

    @Value("${aligo.api.userid}")
    private String userId;

    @Value("${aligo.kakao.plusid}")
    private String plusId;

    @Value("${aligo.kakao.admin-phone}")
    private String adminPhone;

    @Value("${aligo.kakao.auth-url}")
    private String authUrl;

    public Map<String, Object> requestChannelAuth() {
        // 1. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 2. 파라미터 구성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("apikey", apiKey);
        params.add("userid", userId);
        params.add("plusid", plusId);
        params.add("phonenumber", adminPhone);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 3. API 호출 및 응답 반환
        try {
            return restTemplate.postForObject(authUrl, request, Map.class);
        } catch (Exception e) {
            return Map.of("code", -1, "message", "연동 실패: " + e.getMessage());
        }
    }
}
