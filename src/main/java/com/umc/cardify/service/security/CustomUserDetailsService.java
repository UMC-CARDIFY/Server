package com.umc.cardify.service.security;

import com.umc.cardify.domain.User;
import com.umc.cardify.dto.user.UserResponse;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.jwt.JwtUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CustomUserDetailsService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
    }

//    public UserResponse.TokenInfo generateToken(Authentication authentication) {
//        String email = authentication.getName();
//        User user = userRepository.findByEmail(email).orElseThrow();
//
//        String accessToken = jwtUtil.createAccessToken(user.getUserId());
//        String refreshToken = jwtUtil.createRefreshToken(user.getUserId());
//
//        return UserResponse.TokenInfo.builder()
//                .grantType("Bearer")
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .build();
//    }
}

