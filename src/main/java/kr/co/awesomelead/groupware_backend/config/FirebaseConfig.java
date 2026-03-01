package kr.co.awesomelead.groupware_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_CREDENTIALS_JSON:#{null}}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void initialize() {
        try {
            if ((firebaseCredentialsJson == null || firebaseCredentialsJson.isBlank())
                && !new ClassPathResource(
                "firebase/serviceAccountKey.json").exists()) {
                log.warn("⚠️ Firebase 자격증명이 없어 초기화를 건너뜁니다. (테스트 환경일 가능성 높음)");
                return;
            }
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = loadCredentials();
                FirebaseOptions options =
                    FirebaseOptions.builder().setCredentials(credentials).build();
                FirebaseApp.initializeApp(options);
                log.info(
                    "FirebaseApp 초기화 완료 (환경: {})",
                    firebaseCredentialsJson != null ? "운영(Environment)" : "로컬(Classpath)");
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("Firebase 자격증명을 로드할 수 없습니다.", e);
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isBlank()) {
            log.info("Firebase 자격증명: 환경변수(FIREBASE_CREDENTIALS_JSON) 사용");
            byte[] bytes = firebaseCredentialsJson.trim().getBytes(StandardCharsets.UTF_8);
            return GoogleCredentials.fromStream(new ByteArrayInputStream(bytes));
        }

        log.info("Firebase 자격증명: 로컬 파일(classpath:firebase/serviceAccountKey.json) 사용");
        InputStream stream =
            new ClassPathResource("firebase/serviceAccountKey.json").getInputStream();
        return GoogleCredentials.fromStream(stream);
    }
}
