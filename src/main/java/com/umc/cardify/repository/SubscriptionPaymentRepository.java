package com.umc.cardify.repository;

import com.umc.cardify.domain.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
  List<SubscriptionPayment> findBySubscriptionIdOrderByPaidAtDesc(Long subscriptionId);

  Optional<SubscriptionPayment> findByMerchantUid(String merchantUid);
}