package com.umc.cardify.config;

import com.umc.cardify.handler.CustomAuthenticationSuccessHandler;
import com.umc.cardify.jwt.JwtFilter;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.service.security.CustomUserDetailsService;
import com.umc.cardify.service.security.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService, CorsConfigurationSource corsConfigurationSource, UserRepository userRepository, CustomOAuth2UserService customOAuth2UserService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.userRepository = userRepository;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성화
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/oauth2/callback/kakao","/login", "/home", "/loginFailure","/api/v1/users/**", "/swagger-ui/**", "/v3/api-docs/**", "/oauth2/authorization/kakao", "/error").permitAll() // 인증 없이 접근 가능
                                .anyRequest().authenticated() // 나머지 애들은 인증 필요
                )
                .oauth2Login(oauth2Login ->
                        oauth2Login
                                .loginPage("/login")
                                .defaultSuccessUrl("/home")
                                .failureUrl("/loginFailure")
                                .authorizationEndpoint(authorizationEndpoint ->
                                        authorizationEndpoint.baseUri("/oauth2/authorization")
                                )
                                .redirectionEndpoint(redirectionEndpoint ->
                                        redirectionEndpoint.baseUri("/oauth2/callback/*")
                                )
                                .userInfoEndpoint(userInfoEndpoint ->
                                        userInfoEndpoint.userService(customOAuth2UserService)
                                )
                                .successHandler(authenticationSuccessHandler())
                )
                .addFilterBefore(new JwtFilter(jwtUtil, customUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource)); // CORS 설정 적용

        return http.build();
    }

    @Bean
    public CustomAuthenticationSuccessHandler authenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler(jwtUtil, userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
