package com.umc.cardify.service.alert;

import com.umc.cardify.domain.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.discord.enabled", havingValue = "true")
public class DiscordAlertService implements AlertService {

  @Value("${notification.discord.webhook-url}")
  private String webhookUrl;

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public void sendPaymentFailureAlert(Subscription subscription) {
    try {
      String message = buildDiscordMessage(subscription);
      sendToDiscord(message);
    } catch (Exception e) {
      log.error("디스코드 알림 전송 실패: subscriptionId={}, error={}",
          subscription.getId(), e.getMessage(), e);
    }
  }

  private String buildDiscordMessage(Subscription subscription) {
    return String.format(
        "🚨 **구독 결제 10번 연속 실패** 🚨\n" +
            "• **구독 ID**: %d\n" +
            "• **사용자 ID**: %s\n" +
            "• **상품명**: %s\n" +
            "• **결제 예정일**: %s\n" +
            "• **발생 시간**: %s",
        subscription.getId(),
        subscription.getUser().getUserId(),
        subscription.getProduct().getName(),
        subscription.getNextPaymentDate(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    );
  }

  private void sendToDiscord(String message) {
    Map<String, String> payload = Map.of("content", message);
    restTemplate.postForObject(webhookUrl, payload, String.class);
  }
}