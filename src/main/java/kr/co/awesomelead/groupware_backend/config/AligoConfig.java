package kr.co.awesomelead.groupware_backend.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aligo")
@Getter
@Setter
public class AligoConfig {

    private Api api;
    private Kakao kakao;

    @Getter
    @Setter
    public static class Api {

        private String key;
        private String userid;
    }

    @Getter
    @Setter
    public static class Kakao {

        private String plusid;
        private String adminPhone;
        private String senderkey;
        private String authUrl;
        private String sendUrl;
    }
}
