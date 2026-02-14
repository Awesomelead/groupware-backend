package kr.co.awesomelead.groupware_backend.config;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.awesomelead.groupware_backend.domain.auth.filter.JwtFilter;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    private static final String[] SWAGGER_PATHS = {
        "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**"
    };

    // AuthenticationManager Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
        throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 비밀번호 암호화
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // 스웨거 경로들은 Spring Security 필터 체인을 완전히 거치지 않도록 설정합니다.
        return (web) -> web.ignoring().requestMatchers(SWAGGER_PATHS);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf((auth) -> auth.disable());
        http.formLogin((auth) -> auth.disable());
        http.httpBasic((auth) -> auth.disable());

        // 인증 실패 시 401로 고정 (기본 403로 떨어지는 현상 방지)
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
            })
            // 권한 부족(진짜 403 상황)도 명확히 403으로
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
            })
        );

        http.authorizeHttpRequests(
            (auth) ->
                auth.requestMatchers(
                        "/",
                        "/index.html",
                        "/api/test/**",
                        "/api/auth/login",
                        "/api/auth/signup",
                        "/api/auth/reissue",
                        "/api/auth/user",
                        "/api/reissue",
                        "/api/auth/verify-phone-code",
                        "/api/auth/verify-identity",
                        "/api/auth/verify-email-code",
                        "/api/auth/send-phone-code",
                        "/api/auth/send-email-code",
                        "/api/auth/find-email",
                        "/api/auth/reset-password/phone",
                        "/api/auth/reset-password/email",
                        "/api/edu-reports/attachments/{id}/download", // 테스트용으로 교육자료
                        // 다운로드 열어놓음
                        "/api/test/**",
                        "/api/departments/hierarchy",
                        "/api/departments/{departmentId}/users",
                        "/api/visits/**") // 내방객이 본인 방문기록 조회
                    .permitAll()
                    .requestMatchers(
                        "/api/auth/logout",
                        "/api/auth/reset-password", // 로그인 상태에서의 비번 변경
                        "/api/admin/**", // 관리자 기능
                        "/api/visits/admin/**",
                        "/api/visits/check-out")
                    .authenticated()
                    .anyRequest()
                    .authenticated());

        http.addFilterBefore(
            new JwtFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);

        http.sessionManagement(
            (session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
