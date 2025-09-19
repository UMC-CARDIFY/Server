package com.umc.cardify.service.alert;

import com.umc.cardify.domain.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true")
public class EmailAlertService implements AlertService {

  private final JavaMailSender mailSender;

  @Value("${notification.email.admin-emails}")
  private List<String> adminEmails;

  @Value("${spring.mail.username}")
  private String fromEmail;

  public EmailAlertService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void sendPaymentFailureAlert(Subscription subscription) {
    try {
      String subject = "[결제 실패 알림] 구독 ID: " + subscription.getId();
      String content = buildEmailContent(subscription);

      for (String adminEmail : adminEmails) {
        sendEmail(adminEmail, subject, content);
      }

      log.debug("결제 실패 이메일 알림 전송 완료: subscriptionId={}", subscription.getId());
    } catch (Exception e) {
      log.error("이메일 알림 전송 실패: subscriptionId={}, error={}",
          subscription.getId(), e.getMessage());
    }
  }

  private String buildEmailContent(Subscription subscription) {
    return String.format(
        """
        구독 결제가 10번 연속 실패했습니다.
        
        ■ 구독 정보
        - 구독 ID: %d
        - 사용자 ID: %s
        - 상품명: %s
        - 금액: %,d원
        - 결제 예정일: %s
        - 발생 시간: %s
        
        신속한 확인 및 조치가 필요합니다.
        """,
        subscription.getId(),
        subscription.getUser().getUserId(),
        subscription.getProduct().getName(),
        subscription.getProduct().getPrice(),
        subscription.getNextPaymentDate(),
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    );
  }

  private void sendEmail(String to, String subject, String content) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(content);

    mailSender.send(message);
  }
}