package com.umc.cardify.repository;

import com.umc.cardify.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // 결제 수단 목록 조회
    List<PaymentMethod> findByUserId(String userId);

    // 특정 결제 수단 조회
    Optional<PaymentMethod> findByIdAndUserId(Long id, String userId);

    // 기본 결제 수단 조회
    List<PaymentMethod> findByUserIdAndIsDefaultTrue(String userId);

    // 특정 결제 수단 제외한 결제 수단 목록 조회
    List<PaymentMethod> findByUserIdAndIdNot(String userId, Long id);

    // 빌링키로 결제 수단 조회
    Optional<PaymentMethod> findByCustomerId(String customerId);

    // 사용자의 카드 번호로 결제 수단 조회
    Optional<PaymentMethod> findByUserIdAndCardNumber(String userId, String cardNumber);

    // 타입별 결제 수단 조회
    List<PaymentMethod> findByUserIdAndType(String userId, String type);

    // 결제 수단 개수 조회
    long countByUserId(String userId);
}
