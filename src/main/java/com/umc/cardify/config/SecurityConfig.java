package com.umc.cardify.config;

import com.umc.cardify.handler.CustomAuthenticationSuccessHandler;
import com.umc.cardify.jwt.JwtFilter;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.service.security.CustomOAuth2UserService;
import com.umc.cardify.service.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final DefaultOAuth2UserService defaultOAuth2UserService;

    public SecurityConfig(JwtUtil jwtUtil, UserRepository userRepository, CustomUserDetailsService customUserDetailsService, DefaultOAuth2UserService defaultOAuth2UserService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.defaultOAuth2UserService = defaultOAuth2UserService;
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
//                .authorizeRequests(authorizeRequests ->
//                        authorizeRequests
//                                .requestMatchers("/api/v1/users/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // 인증 없이 접근 가능
//                                .anyRequest().authenticated() // 나머지 애들은 인증 필요
//                )
//                .oauth2Login(oauth2Login ->
//                        oauth2Login
//                                .defaultSuccessUrl("/", true) // 로그인 성공 후 메인 페이지로 리디렉션
//                                .userInfoEndpoint(userInfoEndpoint ->
//                                        userInfoEndpoint
//                                                .userService(customOAuth2UserService()) // 사용자 정보 처리 서비스
//                                )
//                                .successHandler(authenticationSuccessHandler()) // 성공 핸들러
//                );
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/v1/users/**", "/swagger-ui/**", "/v3/api-docs/**", "/oauth2/authorization/kakao").permitAll() // 인증 없이 접근 가능
                                .anyRequest().authenticated() // 나머지 애들은 인증 필요
                )
                .addFilterBefore(new JwtFilter(jwtUtil, customUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth2/callback/*"))
                        .userInfoEndpoint(endpoint -> endpoint.userService(defaultOAuth2UserService))
                );

        return http.build();
    }



//    @Bean
//    public CustomOAuth2UserService customOAuth2UserService() {
//        return new CustomOAuth2UserService(userRepository);
//    }

    @Bean
    public CustomAuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler(jwtUtil, userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean(name = "defaultOAuth2UserService")
    public DefaultOAuth2UserService defaultOAuth2UserService() {
        return new DefaultOAuth2UserService();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
//    @Bean
//    public CustomUserDetailsService customUserDetailsService(UserRepository userRepository, JwtUtil jwtUtil) {
//        return new CustomUserDetailsService(userRepository, jwtUtil);
//    }
}