package com.umc.cardify.repository;

import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // 결제 수단 목록 조회
    List<PaymentMethod> findByUser_UserId(Long userId);

    // 특정 결제 수단 조회
    Optional<PaymentMethod> findByIdAndUser_UserId(Long id, Long userId);

    // 기본 결제 수단 조회
    List<PaymentMethod> findByUser_UserIdAndIsDefaultTrue(Long userId);

    // 특정 결제 수단 제외한 결제 수단 목록 조회
    List<PaymentMethod> findByUser_UserIdAndIdNot(Long userId, Long id);

    // 사용자의 카드 번호로 결제 수단 조회
    boolean existsByUserAndCardNumber(User user, String cardNumber);

    // 타입별 결제 수단 조회
    List<PaymentMethod> findByUser_UserIdAndType(Long userId, PaymentType type);

    // 결제 수단 개수 조회
    long countByUser_UserId(Long userId);

    List<PaymentMethod> findByUser_UserIdAndDeletedAtIsNull(Long userId);

    Optional<PaymentMethod> findByUser_UserIdAndIsDefaultTrueAndDeletedAtIsNull(Long userId);

    List<PaymentMethod> findByUser_UserIdAndIdNotAndDeletedAtIsNull(Long userId, Long id);
}