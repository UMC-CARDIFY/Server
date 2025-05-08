package com.umc.cardify.repository;

import com.umc.cardify.domain.Subscription;
import com.umc.cardify.domain.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
  // User 엔티티를 통한 조회 메소드
  List<Subscription> findByUser_UserId(Long userId);

  // 정기결제 예정 구독 조회 메소드
  List<Subscription> findByStatusAndAutoRenewTrueAndNextPaymentDateBetween(
      String status, LocalDateTime startDate, LocalDateTime endDate);

  // 수정이 필요한 메소드 - PaymentMethod와의 관계가 없으므로 @Query 사용
  @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s " +
      "JOIN SubscriptionPayment sp ON sp.subscription.id = s.id " +
      "JOIN PaymentMethod pm ON sp.paymentMethod.id = pm.id " +
      "WHERE pm.id = :paymentMethodId AND s.status IN :statuses")
  boolean existsByPaymentMethodAndStatusIn(@Param("paymentMethodId") Long paymentMethodId,
                                           @Param("statuses") List<String> statuses);

  Long user(User user);
}