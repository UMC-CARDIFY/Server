package com.umc.cardify.service.alert;

import com.umc.cardify.domain.Subscription;

public interface AlertService {
  void sendPaymentFailureAlert(Subscription subscription);
}