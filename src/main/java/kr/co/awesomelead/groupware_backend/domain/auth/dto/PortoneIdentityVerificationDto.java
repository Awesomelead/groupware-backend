package kr.co.awesomelead.groupware_backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 필드 무시
public class PortoneIdentityVerificationDto {

    private String id; // identityVerificationId
    private String status;

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("verifiedCustomer")
    private VerifiedCustomer verifiedCustomer;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {

        private String type;
        private String id;
        private String key;
        private String name;
        private String pgProvider;
        private String pgMerchantId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifiedCustomer {

        private String id; // 고객 식별자

        private String name;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("birthDate")
        private String birthDate;

        private String gender;

        private String ci; // 연계정보

        private String di; // 중복가입확인정보

        @JsonProperty("isForeigner")
        private Boolean isForeigner; // 외국인 여부

        private String operator; // 통신사
    }
}
