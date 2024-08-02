package com.umc.cardify.service.security;

import com.umc.cardify.domain.User;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.jwt.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CustomUserDetailsService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // userId는 문자열로 전달되므로, Long으로 변환
        Long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Invalid userId format: " + userId);
        }

        // userId로 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + userId));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()) // 여전히 이메일을 사용자 이름으로 사용
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))) // 권한 설정
                .build();
    }

}
