package com.umc.cardify.service.security;

import com.umc.cardify.domain.User;
import com.umc.cardify.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        
        Long id;
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("유저 아이디 파싱 실패: " + userId);
        }

        // userId로 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("유저 아이디로 조회 불가능: " + userId));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()) // 여전히 이메일을 사용자 이름으로 사용
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))) // 권한 설정
                .build();
    }

}
