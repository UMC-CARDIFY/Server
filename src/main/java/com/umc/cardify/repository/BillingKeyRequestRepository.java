package com.umc.cardify.repository;

import com.umc.cardify.domain.BillingKeyRequest;
import com.umc.cardify.domain.enums.BillingKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRequestRepository extends JpaRepository<BillingKeyRequest, Long> {
  Optional<BillingKeyRequest> findByMerchantUid(String merchantUid);
  List<BillingKeyRequest> findByUserUserIdAndStatus(Long userId, BillingKeyStatus status);
}