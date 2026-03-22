package kr.co.awesomelead.groupware_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableJpaAuditing
@EnableRetry
@EnableScheduling
@SpringBootApplication
public class GroupwareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupwareBackendApplication.class, args);
    }
}
