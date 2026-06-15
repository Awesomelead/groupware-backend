package kr.co.awesomelead.groupware_backend.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Slf4j
@Configuration
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean startTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean startTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean sslEnable;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private int writeTimeout;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        boolean effectiveSslEnable = sslEnable || port == 465;
        boolean effectiveStartTlsEnable = port == 465 ? false : startTlsEnable;
        boolean effectiveStartTlsRequired = port == 465 ? false : startTlsRequired;

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(effectiveStartTlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(effectiveStartTlsRequired));
        props.put("mail.smtp.ssl.enable", String.valueOf(effectiveSslEnable));
        props.put("mail.debug", "false");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));

        log.info(
                "SMTP mail config - host: {}, port: {}, username: {}, auth: {}, starttls.enable:"
                        + " {}, starttls.required: {}, ssl.enable: {}, timeout: {}/{}/{}ms",
                host,
                port,
                username,
                smtpAuth,
                effectiveStartTlsEnable,
                effectiveStartTlsRequired,
                effectiveSslEnable,
                connectionTimeout,
                timeout,
                writeTimeout);

        return mailSender;
    }
}
