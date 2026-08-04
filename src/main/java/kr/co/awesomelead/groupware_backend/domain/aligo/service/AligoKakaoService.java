package kr.co.awesomelead.groupware_backend.domain.aligo.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.awesomelead.groupware_backend.domain.aligo.dto.response.AligoResponse;
import kr.co.awesomelead.groupware_backend.domain.aligo.dto.response.AligoSmsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AligoKakaoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aligo.api.url}")
    private String apiUrl;

    @Value("${aligo.api.sms-url:https://apis.aligo.in/send/}")
    private String smsApiUrl;

    @Value("${aligo.api.apikey}")
    private String apiKey;

    @Value("${aligo.api.userid}")
    private String userId;

    @Value("${aligo.api.senderkey}")
    private String senderKey;

    @Value("${aligo.api.sender}")
    private String sender;

    @Value("${aligo.api.tpl-code}")
    private String tplCode;

    @Value("${aligo.api.test-mode:false}")
    private boolean testMode;

    /** 회원가입 인증번호 알림톡 전송 */
    public boolean sendAuthCodeAlimtalk(String phoneNumber, String authCode) {

        // 테스트 모드일 경우 실제 전송 없이 성공 반환
        if (testMode) {
            log.info("🧪 [테스트 모드] 알림톡 전송 생략 - 전화번호: {}, 인증번호: {}", phoneNumber, authCode);
            return true;
        }

        try {
            // 템플릿: "[어썸그룹] 인증번호는 #{인증번호}입니다."
            String message = String.format("[어썸그룹] 인증번호는 %s입니다.", authCode);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("apikey", apiKey);
            params.add("userid", userId);
            params.add("senderkey", senderKey);
            params.add("tpl_code", tplCode);
            params.add("sender", sender);

            // 수신자 정보
            params.add("receiver_1", phoneNumber);
            params.add("subject_1", "회원가입 인증번호");
            params.add("message_1", message);

            // 실패시 문자 전송
            params.add("failover", "Y");
            params.add("fsubject_1", "회원가입 인증번호");
            params.add("fmessage_1", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(apiUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                AligoResponse aligoResponse =
                        objectMapper.readValue(response.getBody(), AligoResponse.class);

                log.info(
                        "알림톡 전송 결과 - code: {}, message: {}",
                        aligoResponse.getCode(),
                        aligoResponse.getMessage());

                return aligoResponse.getCode() == 0;
            }

            log.error("알림톡 API 호출 실패 - status: {}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            log.error("알림톡 전송 중 예외 발생", e);
            return false;
        }
    }

    /** 회원가입 인증번호 문자 전송 */
    public boolean sendAuthCodeSms(String phoneNumber, String authCode) {

        if (testMode) {
            log.info("🧪 [테스트 모드] 문자 전송 생략 - 전화번호: {}", maskPhoneNumber(phoneNumber));
            return true;
        }

        try {
            String message = String.format("[어썸그룹] 인증번호는 %s입니다.", authCode);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);
            params.add("sender", sender);
            params.add("receiver", phoneNumber);
            params.add("msg", message);
            params.add("msg_type", "SMS");
            params.add("title", "회원가입 인증번호");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.info(
                    "문자 전송 요청 - url: {}, userId: {}, sender: {}, receiver: {}, msgType: {}",
                    smsApiUrl,
                    userId,
                    maskPhoneNumber(sender),
                    maskPhoneNumber(phoneNumber),
                    "SMS");

            ResponseEntity<String> response =
                    restTemplate.postForEntity(smsApiUrl, request, String.class);

            log.info(
                    "문자 API 응답 수신 - status: {}, body: {}",
                    response.getStatusCode(),
                    response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                AligoSmsResponse smsResponse =
                        objectMapper.readValue(response.getBody(), AligoSmsResponse.class);

                log.info(
                        "문자 전송 결과 - resultCode: {}, message: {}, messageId: {},"
                                + " successCount: {}, errorCount: {}, messageType: {}",
                        smsResponse.getResultCode(),
                        smsResponse.getMessage(),
                        smsResponse.getMessageId(),
                        smsResponse.getSuccessCount(),
                        smsResponse.getErrorCount(),
                        smsResponse.getMessageType());

                return Integer.valueOf(1).equals(smsResponse.getResultCode())
                        && smsResponse.getSuccessCount() != null
                        && smsResponse.getSuccessCount() > 0
                        && (smsResponse.getErrorCount() == null
                                || smsResponse.getErrorCount() == 0);
            }

            log.error("문자 API 호출 실패 - status: {}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            log.error(
                    "문자 전송 중 예외 발생 - url: {}, userId: {}, sender: {}, receiver: {}",
                    smsApiUrl,
                    userId,
                    maskPhoneNumber(sender),
                    maskPhoneNumber(phoneNumber),
                    e);
            return false;
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber;
        }

        return phoneNumber.substring(0, 3)
                + "****"
                + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
