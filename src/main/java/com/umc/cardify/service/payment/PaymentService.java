package com.umc.cardify.service.payment;

import com.umc.cardify.dto.payment.billing.BillingKeyRequestDTO;
import com.umc.cardify.dto.payment.billing.BillingKeyResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;

public interface PaymentService {

  // 빌링키 관련
  BillingKeyResponse.RequestBillingKeyRes requestBillingKey(BillingKeyRequestDTO.RequestBillingKeyReq request);
  BillingKeyResponse.VerifyBillingKeyRes verifyAndSaveBillingKey(BillingKeyRequestDTO.VerifyBillingKeyReq request);
  BillingKeyResponse.ApproveBillingKeyRes approveBillingKey(BillingKeyRequestDTO.ApproveBillingKeyReq request);
  BillingKeyResponse.BillingStatusRes getBillingStatus(String merchantUid);

  // 결제 관련
  boolean cancelPayment(SubscriptionRequest.CancelPaymentReq request);

  // 정기 결제 처리
  void processRecurringPayments();

  // 웹훅 처리
  void handleWebhook(WebhookRequest request);
}