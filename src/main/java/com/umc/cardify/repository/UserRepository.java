package com.umc.cardify.repository;

import com.umc.cardify.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email); // 중복 검사 메서드

    Optional<User> findByEmailAndKakao(String email, boolean kakao);

}
