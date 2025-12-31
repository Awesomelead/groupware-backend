package kr.co.awesomelead.groupware_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Bean
    public S3Client s3Client() {

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(credentials);

        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(credentials);
        return S3Presigner.builder()
            .region(Region.of(region)) // 동일한 리전 설정
            .credentialsProvider(credentialsProvider) // 동일한 자격 증명 사용
            .build();
    }
}