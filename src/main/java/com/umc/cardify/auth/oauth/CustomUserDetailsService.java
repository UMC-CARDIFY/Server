package com.umc.cardify.auth.oauth;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 토큰에서 이메일과 제공자 추출하는 방식으로 수정
    @Override
    public UserDetails loadUserByUsername(String token) throws UsernameNotFoundException {

        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "",  // 소셜 로그인이므로 password 불필요
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}