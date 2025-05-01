package com.umc.cardify.service.payment;

import com.umc.cardify.dto.payment.billing.BillingKeyRequest;
import com.umc.cardify.dto.payment.billing.BillingKeyResponse;
import com.umc.cardify.dto.payment.method.PaymentMethodResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;

import java.util.List;

public interface PaymentService {
  // 빌링키 관련
  BillingKeyResponse.RequestBillingKeyRes requestBillingKey(BillingKeyRequest.RequestBillingKeyReq request);
  BillingKeyResponse.VerifyBillingKeyRes verifyAndSaveBillingKey(BillingKeyRequest.VerifyBillingKeyReq request);

  // 결제 수단 관련
  PaymentMethodResponse.PaymentMethodInfoRes getPaymentMethod(Long paymentMethodId);
  PaymentMethodResponse.PaymentMethodListRes getPaymentMethodsByUserId(Long userId);
  boolean deletePaymentMethod(Long paymentMethodId);
  boolean setDefaultPaymentMethod(Long paymentMethodId);

  // 구독 관련
  SubscriptionResponse.SubscriptionInfoRes createSubscription(SubscriptionRequest.CreateSubscriptionReq request);
  SubscriptionResponse.SubscriptionInfoRes getSubscription(Long subscriptionId);
  SubscriptionResponse.SubscriptionListRes getSubscriptionsByUserId(Long userId);
  boolean cancelSubscription(SubscriptionRequest.CancelSubscriptionReq request);
  boolean updateAutoRenew(Long subscriptionId, boolean autoRenew);

  // 결제 관련
  SubscriptionResponse.PaymentHistoryListRes getPaymentHistoriesBySubscriptionId(Long subscriptionId);
  boolean cancelPayment(SubscriptionRequest.CancelPaymentReq request);

  // 정기 결제 처리
  void processRecurringPayments();

  // 웹훅 처리
  void handleWebhook(WebhookRequest request);
}