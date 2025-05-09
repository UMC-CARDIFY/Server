package com.umc.cardify.repository;

import com.umc.cardify.domain.SubscriptionPayment;
import com.umc.cardify.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
  List<SubscriptionPayment> findBySubscriptionIdOrderByPaidAtDesc(Long subscriptionId);

  Optional<SubscriptionPayment> findByMerchantUid(String merchantUid);

  /**
   * 특정 구독의 가장 최근 결제 내역을 조회합니다.
   *
   * @param subscriptionId 구독 ID
   * @param status 결제 상태 (PAID, FAILED 등)
   * @return 최근 결제 내역 목록 (최대 1개)
   */
  List<SubscriptionPayment> findTop1BySubscriptionIdAndStatusOrderByCreatedAtDesc(
      Long subscriptionId, PaymentStatus status);
}