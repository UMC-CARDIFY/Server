package com.umc.cardify.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.umc.cardify.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByUserId(Long userId);

	boolean existsByEmail(String email); // 중복 검사 메서드

	Optional<User> findByEmailAndKakao(String email, boolean kakao);

	@Modifying
	@Transactional
	@Query("UPDATE User u SET u.todayCheck = 0")
	void resetAllTodayCheck();
}
