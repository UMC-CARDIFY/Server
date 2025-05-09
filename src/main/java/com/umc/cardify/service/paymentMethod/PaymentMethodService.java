package com.umc.cardify.service.paymentMethod;

import com.umc.cardify.dto.payment.method.PaymentMethodRequest;
import com.umc.cardify.dto.payment.method.PaymentMethodResponse;

import java.util.List;

public interface PaymentMethodService {

  PaymentMethodResponse.PaymentMethodInfoRes createPaymentMethod(PaymentMethodRequest.RegisterPaymentReq request, String token);
  List<PaymentMethodResponse.PaymentMethodInfoRes> getPaymentMethods(String token);
  void deletePaymentMethod(Long id, String token);
  PaymentMethodResponse.PaymentMethodInfoRes setDefaultPaymentMethod(Long id, String token);
  void clearDefaultPaymentMethod(Long userId);
}
