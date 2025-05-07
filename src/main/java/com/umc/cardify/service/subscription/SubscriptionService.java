package com.umc.cardify.service.subscription;

import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse;

public interface SubscriptionService {

  // 구독 관련
  SubscriptionResponse.SubscriptionInfoRes createSubscription(SubscriptionRequest.CreateSubscriptionReq request, String token);
  SubscriptionResponse.SubscriptionInfoRes getSubscription(Long subscriptionId, String token);
  SubscriptionResponse.SubscriptionListRes getSubscriptionsByUserId(String token);
  boolean cancelSubscription(SubscriptionRequest.CancelSubscriptionReq request, String token);
  boolean updateAutoRenew(Long subscriptionId, boolean autoRenew, String token);
  SubscriptionResponse.PaymentHistoryListRes getPaymentHistoriesBySubscriptionId(Long subscriptionId, String token);
}
