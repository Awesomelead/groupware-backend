package kr.co.awesomelead.groupware_backend.config;

import kr.co.awesomelead.groupware_backend.auth.filter.JwtFilter;
import kr.co.awesomelead.groupware_backend.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

        http.authorizeHttpRequests(
                (auth) ->
                        auth
                                // 테스트용으로 어드민 경로도 열어놓음
                                .requestMatchers(
                                        "/",
                                        "/index.html",
                                        "/api/join",
                                        "/api/auth/login",
                                        "/api/reissue",
                                        "/api/admin/**")
                                .permitAll()
                                // .requestMatchers("/api/join", "/api/auth/login",
                                // "/api/reissue").permitAll()
                                // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .anyRequest()
                                .authenticated());

        http.addFilterBefore(
                new JwtFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);

        http.sessionManagement(
                (session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
