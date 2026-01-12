package kr.co.awesomelead.groupware_backend.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.PortoneIdentityVerificationDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.IdentityVerificationResponseDto;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${portone.apisecret}")
    private String portoneApiSecret;

    private static final String PORTONE_API_URL = "https://api.portone.io/identity-verifications/";

    /** 포트원 본인인증 정보 조회 */
    public IdentityVerificationResponseDto verifyIdentity(String identityVerificationId) {
        try {
            // URL 인코딩
            String encodedId =
                    URLEncoder.encode(identityVerificationId, StandardCharsets.UTF_8.toString());
            String url = PORTONE_API_URL + encodedId;

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "PortOne " + portoneApiSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // API 호출
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            // 404 Not Found - 존재하지 않는 identityVerificationId
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("본인인증 정보를 찾을 수 없음: id={}", identityVerificationId);
                throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_NOT_FOUND);
            }

            // 기타 에러 응답
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error(
                        "포트원 API 호출 실패: status={}, body={}",
                        response.getStatusCode(),
                        response.getBody());
                throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
            }

            // 응답 파싱
            PortoneIdentityVerificationDto portoneResponse =
                    objectMapper.readValue(
                            response.getBody(), PortoneIdentityVerificationDto.class);

            // 인증 상태 확인 (VERIFIED가 아닌 경우: READY, FAILED 등)
            if (!"VERIFIED".equals(portoneResponse.getStatus())) {
                log.warn(
                        "본인인증 미완료: status={}, id={}",
                        portoneResponse.getStatus(),
                        identityVerificationId);
                throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_NOT_COMPLETED);
            }

            // DTO 변환
            PortoneIdentityVerificationDto.VerifiedCustomer customer =
                    portoneResponse.getVerifiedCustomer();

            return IdentityVerificationResponseDto.builder()
                    .identityVerificationId(identityVerificationId)
                    .status(portoneResponse.getStatus())
                    .name(customer.getName())
                    .phoneNumber(customer.getPhoneNumber())
                    .birthDate(customer.getBirthDate())
                    .gender(customer.getGender())
                    .build();

        } catch (CustomException e) {
            // CustomException은 그대로 던짐
            throw e;
        } catch (UnsupportedEncodingException e) {
            log.error("URL 인코딩 실패", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("본인인증 조회 중 예상치 못한 예외 발생: id={}", identityVerificationId, e);
            throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }
}
