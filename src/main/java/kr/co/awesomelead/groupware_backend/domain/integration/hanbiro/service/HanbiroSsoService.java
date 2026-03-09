package kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.dto.request.HanbiroAccountLinkRequestDto;
import kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.dto.response.HanbiroMailRedirectResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HanbiroSsoService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${hanbiro.sso.base-url}")
    private String baseUrl;

    @Value("${hanbiro.sso.auth-path:/auth}")
    private String authPath;

    @Value("${hanbiro.sso.create-token-path:/create_token}")
    private String createTokenPath;

    @Value("${hanbiro.sso.raw:1}")
    private String raw;

    @Value("${hanbiro.sso.method:mail}")
    private String method;

    @Transactional
    public void linkAccount(Long userId, HanbiroAccountLinkRequestDto requestDto) {
        validateBaseConfig();

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        HanbiroAuthResponse authResponse =
                callAuth(requestDto.getHanbiroId().trim(), requestDto.getHanbiroPassword().trim());
        if (!Boolean.TRUE.equals(authResponse.getSuccess()) || isBlank(authResponse.getSession())) {
            throw new CustomException(ErrorCode.HANBIRO_REAUTH_REQUIRED);
        }

        user.setHanbiroId(requestDto.getHanbiroId().trim());
        user.setHanbiroPassword(requestDto.getHanbiroPassword().trim());
        user.setHanbiroLinkedAt(LocalDateTime.now());
    }

    @Transactional
    public HanbiroMailRedirectResponseDto createMailRedirectUri(Long userId) {
        validateBaseConfig();

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (isBlank(user.getHanbiroId()) || isBlank(user.getHanbiroPassword())) {
            throw new CustomException(ErrorCode.HANBIRO_ACCOUNT_NOT_LINKED);
        }

        HanbiroAuthResponse authResponse = callAuth(user.getHanbiroId(), user.getHanbiroPassword());
        if (!Boolean.TRUE.equals(authResponse.getSuccess())
                || authResponse.getSession() == null
                || authResponse.getSession().isBlank()) {
            // 비밀번호 변경/잠김 등으로 인증 실패하면 재인증 강제
            user.setHanbiroPassword(null);
            user.setHanbiroLinkedAt(null);
            throw new CustomException(ErrorCode.HANBIRO_REAUTH_REQUIRED);
        }

        HanbiroCreateTokenResponse createTokenResponse = callCreateToken(authResponse.getSession());
        if (!Boolean.TRUE.equals(createTokenResponse.getSuccess())
                || createTokenResponse.getRedirectUri() == null
                || createTokenResponse.getRedirectUri().isBlank()) {
            throw new CustomException(ErrorCode.HANBIRO_SSO_FAILED);
        }

        return new HanbiroMailRedirectResponseDto(createTokenResponse.getRedirectUri());
    }

    private HanbiroAuthResponse callAuth(String gwId, String gwPass) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("gw_id", gwId);
        body.add("gw_pass", gwPass);
        body.add("raw", raw);

        try {
            ResponseEntity<HanbiroAuthResponse> response =
                    restTemplate.postForEntity(
                            buildUrl(authPath), createFormRequest(body), HanbiroAuthResponse.class);
            return Objects.requireNonNullElseGet(response.getBody(), HanbiroAuthResponse::new);
        } catch (RestClientException e) {
            log.error("Hanbiro /auth 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.HANBIRO_SSO_FAILED);
        }
    }

    private HanbiroCreateTokenResponse callCreateToken(String sessionId) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("session_id", sessionId);
        body.add("method", method);

        try {
            ResponseEntity<HanbiroCreateTokenResponse> response =
                    restTemplate.postForEntity(
                            buildUrl(createTokenPath),
                            createFormRequest(body),
                            HanbiroCreateTokenResponse.class);
            return Objects.requireNonNullElseGet(
                    response.getBody(), HanbiroCreateTokenResponse::new);
        } catch (RestClientException e) {
            log.error("Hanbiro /create_token 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.HANBIRO_SSO_FAILED);
        }
    }

    private HttpEntity<MultiValueMap<String, String>> createFormRequest(
            MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(body, headers);
    }

    private String buildUrl(String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private void validateBaseConfig() {
        if (isBlank(baseUrl)) {
            throw new CustomException(ErrorCode.HANBIRO_SSO_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HanbiroAuthResponse {
        private Boolean success;
        private String msg;
        private String jwt;
        private String session;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HanbiroCreateTokenResponse {
        private Boolean success;
        private String msg;
        private String authKey;

        @JsonProperty("redirect_uri")
        private String redirectUri;
    }
}
