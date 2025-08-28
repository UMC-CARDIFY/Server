package com.umc.cardify.service.alert;

import com.umc.cardify.domain.Subscription;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

  // TODO : 이후에 메일 서비스 추가를 위해
  private final List<AlertService> alertServices;  // 모든 알림 서비스들

  public NotificationService(List<AlertService> alertServices) {
    this.alertServices = alertServices;
  }

  public void sendPaymentFailureAlert(Subscription subscription) {
    // 모든 알림 서비스에 한 번에 전송
    for (AlertService alertService : alertServices) {
      alertService.sendPaymentFailureAlert(subscription);
    }
  }
}