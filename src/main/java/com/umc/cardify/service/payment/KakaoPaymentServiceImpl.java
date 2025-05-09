package com.umc.cardify.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.*;
import com.umc.cardify.domain.enums.*;
import com.umc.cardify.dto.payment.billing.BillingKeyRequestDTO;
import com.umc.cardify.dto.payment.billing.BillingKeyResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse;
import com.umc.cardify.dto.payment.webhook.WebhookRequest;
import com.umc.cardify.repository.*;
import com.umc.cardify.service.subscription.SubscriptionServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPaymentServiceImpl implements PaymentService {

  private final PortoneClient portoneClient;
  private final PaymentMethodRepository paymentMethodRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final SubscriptionServiceImpl subscriptionServiceImpl;
  private final RestTemplate restTemplate;
  private final BillingKeyRequestRepository billingKeyRequestRepository;

  @Value("${portone.pg_code}")
  private String KAKAOPAY_PG_CODE;

  // 빌링키 요청
  @Override
  @Transactional
  public BillingKeyResponse.RequestBillingKeyRes requestBillingKey(BillingKeyRequestDTO.RequestBillingKeyReq request) {
    log.info("빌링키 요청 시작: userId={}, productId={}", request.userId(), request.productId());

    // 구독 중복 확인
    long activeSubscriptionCount = subscriptionRepository.countByUser_UserIdAndStatus(request.userId(), SubscriptionStatus.ACTIVE);

    if (activeSubscriptionCount > 0) {
      List<BillingKeyRequest> billingKeyRequestList = billingKeyRequestRepository.findByUserUserIdAndStatus(request.userId(), BillingKeyStatus.REQUESTED);
      BillingKeyRequest billingKeyRequest = billingKeyRequestList.get(billingKeyRequestList.size() - 1);
      billingKeyRequest.setStatus(BillingKeyStatus.FAILED);
      throw new IllegalStateException("이미 활성 상태의 구독이 있습니다. 새로운 구독을 시작하기 전에 기존 구독을 취소해 주세요.");
    }

    // 고유 식별자 생성
    String merchantUid = "subscribe_" + UUID.randomUUID().toString();
    String customerUid = "customer_" + request.userId() + "_" + System.currentTimeMillis();

    try {
      // 사용자 및 상품 조회
      User user = userRepository.findById(request.userId())
          .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.userId()));

      Product product = null;
      if (request.productId() != null) {
        product = productRepository.findById(request.productId())
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + request.productId()));
      }

      // 프론트엔드에 제공할 정보 구성 (포트원 JavaScript SDK 호출용)
      Map<String, Object> requestData = new HashMap<>();
      requestData.put("pg", KAKAOPAY_PG_CODE);
      requestData.put("pay_method", "card");
      requestData.put("merchant_uid", merchantUid);
      requestData.put("customer_uid", customerUid);
      requestData.put("name", "구독 서비스 자동결제 등록");
      requestData.put("amount", 0); // 빌링키 발급은 0원
      requestData.put("buyer_email", request.email());
      requestData.put("buyer_name", request.name());

      // 콜백 URL 설정
      String baseUrl = request.callbackUrl() != null ?
          request.callbackUrl() : "http://localhost:8080";

      // TODO: 이후 완성
      // requestData.put("m_redirect_url", baseUrl + "/api/v1/payments/kakaopay/mobile-success"); // 모바일 환경
      requestData.put("success_url", baseUrl + "/api/v1/payments/kakaopay/success");
      requestData.put("fail_url", baseUrl + "/api/v1/payments/kakaopay/fail");

      // 빌링키 요청 정보 저장
      BillingKeyRequest billingKeyRequest = BillingKeyRequest.builder()
          .user(user)
          .merchantUid(merchantUid)
          .customerUid(customerUid)
          .status(BillingKeyStatus.REQUESTED)
          .pgProvider(KAKAOPAY_PG_CODE)
          .requestData(objectMapper.writeValueAsString(requestData))
          .product(product)
          .build();

      billingKeyRequestRepository.save(billingKeyRequest);
      log.info("빌링키 요청 정보 저장 완료: id={}, merchantUid={}", billingKeyRequest.getId(), merchantUid);

      // 프론트엔드 연동용 응답 생성
      return BillingKeyResponse.RequestBillingKeyRes.builder()
          .merchantUid(merchantUid)
          .customerUid(customerUid)
          .requestData(requestData)
          .build();
    } catch (Exception e) {
      log.error("빌링키 요청 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("빌링키 요청 중 오류 발생: " + e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public BillingKeyResponse.ApproveBillingKeyRes approveBillingKey(BillingKeyRequestDTO.ApproveBillingKeyReq request) {
    log.info("빌링키 발급 승인 시작: pgToken={}, merchantUid={}", request.pgToken(), request.merchantUid());

    try {
      // 1. 빌링키 요청 정보 조회
      BillingKeyRequest billingKeyRequest = billingKeyRequestRepository.findByMerchantUid(request.merchantUid())
          .orElseThrow(() -> new RuntimeException("빌링키 요청 정보를 찾을 수 없습니다: " + request.merchantUid()));

      // 2. 포트원 API 호출하여 빌링키 발급 상태 확인
      // 실제 구현에서는 포트원의 API를 호출하여 검증
      // 여기서는 간략화를 위해 생략 (포트원 클라이언트 호출 부분)
      String customerUid = billingKeyRequest.getCustomerUid();
      try {
        log.info("포트원 API 빌링키 승인 호출: customerUid={}, pgToken={}", customerUid, request.pgToken());
      } catch (Exception e) {
        log.error("포트원 API 빌링키 승인 실패: {}", e.getMessage());
        billingKeyRequest.updateStatus(BillingKeyStatus.FAILED);
        throw new RuntimeException("빌링키 발급 승인에 실패했습니다: " + e.getMessage());
      }

      // 3. pg_token 저장 및 상태 업데이트
      billingKeyRequest.setPgToken(request.pgToken());
      billingKeyRequest.updateStatus(BillingKeyStatus.APPROVED);

      // 4. 결제 수단 정보 저장
      Long userId = request.userId();

      // 기존 결제 수단이 있는지 확인하고 default 상태 변경
      List<PaymentMethod> existingMethods = paymentMethodRepository.findByUser_UserIdAndDeletedAtIsNull(userId);
      if (!existingMethods.isEmpty()) {
        for (PaymentMethod method : existingMethods) {
          method.setIsDefault(false);
        }
        paymentMethodRepository.saveAll(existingMethods);
      }

      // 새 결제 수단 저장
      PaymentMethod paymentMethod = PaymentMethod.builder()
          .user(userRepository.findByUserId(userId))
          .type(PaymentType.KAKAO)
          .provider("KAKAOPAY")
          .billingKey(billingKeyRequest.getCustomerUid())
          .isDefault(true) // 결제한 카카오페이를 기본 결제 수단으로
          // 카카오페이는 카드 유효 기간 정보 없음
          // .validUntil(LocalDate.now().plusYears(1))
          .build();

      PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
      log.info("결제 수단 저장 완료: id={}, billingKey={}", savedPaymentMethod.getId(), maskString(customerUid));

      // 빌링키 요청과 결제 수단 연결
      billingKeyRequest.setPaymentMethod(savedPaymentMethod);

      // 구독 생성
      SubscriptionRequest.CreateSubscriptionReq subscriptionRequest = SubscriptionRequest.CreateSubscriptionReq.builder()
          .userId(userId)
          .productId(request.productId())
          .paymentMethodId(savedPaymentMethod.getId())
          .autoRenew(true)
          .pgProvider(billingKeyRequest.getPgProvider())
          .build();

      SubscriptionResponse.SubscriptionInfoRes subscriptionRes =
          subscriptionServiceImpl.createSubscription(subscriptionRequest, null);

      // 6. 응답 구성
      return BillingKeyResponse.ApproveBillingKeyRes.builder()
          .merchantUid(request.merchantUid())
          .customerUid(billingKeyRequest.getCustomerUid())
          .status("success")
          .paymentMethodId(savedPaymentMethod.getId())
          .userId(userId)
          .subscriptionId(subscriptionRes.id())
          .build();
    } catch (Exception e) {
      log.error("빌링키 발급 승인 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("빌링키 발급 승인 중 오류 발생: " + e.getMessage(), e);
    }
  }

  // 민감한 정보 마스킹 유틸리티 메서드
  private String maskString(String input) {
    if (input == null || input.length() < 8) {
      return "****";
    }
    int visibleChars = Math.min(4, input.length() / 4);
    return input.substring(0, visibleChars) +
        "*".repeat(input.length() - 2 * visibleChars) +
        input.substring(input.length() - visibleChars);
  }

  // 빌링키 상태 조회
  @Override
  public BillingKeyResponse.BillingStatusRes getBillingStatus(String merchantUid) {
    log.info("빌링키 상태 조회 시작: merchantUid={}", merchantUid);

    try {
      // 포트원 API를 통해 결제 상태 조회
      String token = portoneClient.getAccessToken();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Authorization", "Bearer " + token);

      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String url = "https://api.iamport.kr/payments/find/" + merchantUid;
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

      // 응답 처리
      JsonNode rootNode = objectMapper.readTree(response.getBody());

      if (rootNode.path("code").asInt() != 0) {
        log.error("빌링키 상태 조회 실패: {}", rootNode.path("message").asText());
        throw new RuntimeException("빌링키 상태 조회 실패: " + rootNode.path("message").asText());
      }

      JsonNode responseData = rootNode.path("response");

      return BillingKeyResponse.BillingStatusRes.builder()
          .merchantUid(merchantUid)
          .status(responseData.path("status").asText())
          .customerUid(responseData.path("customer_uid").asText())
          .build();

    } catch (Exception e) {
      log.error("빌링키 상태 조회 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("빌링키 상태 조회 중 오류 발생: " + e.getMessage(), e);
    }
  }

  // 결체 취소
  @Override
  @Transactional
  public boolean cancelPayment(SubscriptionRequest.CancelPaymentReq request) {
    log.info("결제 취소 요청: merchantUid={}, reason={}", request.merchantUid(), request.reason());

    try {
      // 결제 조회
      SubscriptionPayment subscriptionPayment = subscriptionPaymentRepository.findByMerchantUid(request.merchantUid())
          .orElseThrow(() -> new ResourceNotFoundException("결제 내역을 찾을 수 없습니다: " + request.merchantUid()));

      // 이미 취소된 결제인지 확인
      if ("CANCELLED".equals(subscriptionPayment.getStatus().name())) {
        log.warn("이미 취소된 결제입니다: merchantUid={}", request.merchantUid());
        return false;
      }

      // 결제 취소 요청
      JsonNode cancelResponse = portoneClient.cancelPayment(request.merchantUid(), request.reason());

      // 취소 성공 시 상태 업데이트
      if (cancelResponse == null) {
        subscriptionPayment.setStatus(PaymentStatus.CANCELED);
        subscriptionPaymentRepository.save(subscriptionPayment);

        // 취소 응답 정보 저장 (선택사항)
        try {
          subscriptionPayment.setPgResponse(objectMapper.writeValueAsString(cancelResponse));
        } catch (Exception e) {
          log.warn("취소 응답 저장 실패: {}", e.getMessage());
        }

        log.info("결제 취소 성공: merchantUid={}", request.merchantUid());
        return true;
      } else {
        log.error("결제 취소 실패: 응답이 없습니다");
        return false;
      }

    } catch (Exception e) {
      log.error("결제 취소 중 오류 발생: {}", e.getMessage());
      return false;
    }
  }

  @Override
  @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시에 실행
  @Transactional
  public void processRecurringPayments() {
    log.info("정기 결제 처리 시작");

    // 오늘 결제해야 할 구독 목록 조회
    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<Subscription> subscriptionsDue = subscriptionRepository.findByStatusAndAutoRenewTrueAndNextPaymentDateBetween(
        SubscriptionStatus.ACTIVE,
        startOfDay,
        endOfDay
    );

    log.info("오늘 결제 대상 구독 수: {}", subscriptionsDue.size());

    for (Subscription subscription : subscriptionsDue) {
      try {
        processSubscriptionPayment(subscription);
      } catch (Exception e) {
        log.error("구독 ID {}의 결제 처리 중 오류 발생: {}", subscription.getId(), e.getMessage());
      }
    }

    log.info("정기 결제 처리 완료");
  }

  private void processSubscriptionPayment(Subscription subscription) {
    log.info("구독 ID {}의 결제 처리 시작", subscription.getId());

    try {
      // 상품 정보 조회
      Product product = subscription.getProduct();
      if (product == null) {
        throw new ResourceNotFoundException("상품을 찾을 수 없습니다: " + subscription.getId());
      }

      // 결제 수단 조회 - 구독에 결제 수단이 없으므로 최근 결제 내역의 결제 수단 또는 사용자의 기본 결제 수단 사용
      PaymentMethod paymentMethod = null;

      // 1. 최근 결제 내역의 결제 수단 조회 시도
      List<SubscriptionPayment> recentPayments = subscriptionPaymentRepository
          .findTop1BySubscriptionIdAndStatusOrderByCreatedAtDesc(
              subscription.getId(), PaymentStatus.PAID);

      if (!recentPayments.isEmpty() && recentPayments.get(0).getPaymentMethod() != null) {
        paymentMethod = recentPayments.get(0).getPaymentMethod();
        log.debug("최근 결제 내역의 결제 수단을 사용합니다: {}", paymentMethod.getId());
      } else {
        // 2. 사용자의 기본 결제 수단 조회
        paymentMethod = paymentMethodRepository
            .findByUser_UserIdAndIsDefaultTrueAndDeletedAtIsNull(subscription.getUser().getUserId())
            .orElseThrow(() -> new RuntimeException("결제 수단을 찾을 수 없습니다: " + subscription.getUser().getUserId()));
        log.debug("사용자의 기본 결제 수단을 사용합니다: {}", paymentMethod.getId());
      }

      // 빌링키 확인
      if (paymentMethod.getBillingKey() == null || paymentMethod.getBillingKey().isEmpty()) {
        throw new RuntimeException("결제 수단에 빌링키가 없습니다: " + paymentMethod.getId());
      }

      // 금액 체크
      if (product.getPrice() <= 0) {
        log.warn("상품 가격이 0 이하입니다. 결제를 건너뜁니다: productId={}", product.getId());
        return;
      }

      // 결제 요청
      String merchantUid = "recurring_" + subscription.getId() + "_" + System.currentTimeMillis();

      // 결제 요청 파라미터
      Map<String, String> parameters = new HashMap<>();
      parameters.put("pg", KAKAOPAY_PG_CODE);

      JsonNode response = portoneClient.requestSubscriptionPayment(
          paymentMethod.getBillingKey(),
          merchantUid,
          product.getName(),
          product.getPrice(),
          parameters
      );

      // 응답에서 필요한 정보 추출
      String impUid = response.path("imp_uid").asText();
      JsonNode paymentInfo = portoneClient.getPaymentData(impUid);
      String paymentStatus = paymentInfo.path("status").asText();

      // 결제 결과 저장
      SubscriptionPayment subscriptionPayment = SubscriptionPayment.builder()
          .subscription(subscription)
          .paymentMethod(paymentMethod)  // 사용한 결제 수단 저장
          .merchantUid(merchantUid)
          .amount(product.getPrice())
          .pgProvider(paymentMethod.getProvider())
          .pgResponse(objectMapper.writeValueAsString(response))
          .build();

      // 결제 상태 설정
      if ("paid".equals(paymentStatus)) {
        subscriptionPayment.setStatus(PaymentStatus.PAID);
        subscriptionPayment.setPaidAt(LocalDateTime.now());

        // 다음 결제일 계산 (현재 결제일로부터 한 달 후)
        LocalDateTime currentNextPaymentDate = subscription.getNextPaymentDate();
        LocalDateTime nextPaymentDateTime = currentNextPaymentDate.plusMonths(1);

        // 월말 처리
        int currentDay = currentNextPaymentDate.getDayOfMonth();
        Month nextMonth = nextPaymentDateTime.getMonth();
        int daysInNextMonth = nextMonth.length(Year.isLeap(nextPaymentDateTime.getYear()));

        if (currentDay > daysInNextMonth) {
          // 다음 달의 마지막 날로 조정
          nextPaymentDateTime = nextPaymentDateTime.withDayOfMonth(daysInNextMonth);
        }

        subscription.setNextPaymentDate(nextPaymentDateTime);

        // 구독 종료일 확인 및 자동 갱신 처리
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(LocalDateTime.now())) {
          // 구독 기간 연장
          String period = subscription.getProduct().getPeriod().name();
          LocalDateTime newEndDate = ("YEAR".equals(period)) ?
              subscription.getEndDate().plusYears(1) : subscription.getEndDate().plusMonths(1);

          subscription.setEndDate(newEndDate);
          log.info("구독 기간 연장: id={}, 새 종료일={}", subscription.getId(), newEndDate);
        }

        subscriptionRepository.save(subscription);
        log.info("다음 결제일 업데이트: subscriptionId={}, nextPaymentDate={}",
            subscription.getId(), nextPaymentDateTime);
      } else {
        subscriptionPayment.setStatus(PaymentStatus.FAILED);
        // 결제 실패 처리 - 필요시 알림 발송, 재시도 로직 등 추가
      }

      subscriptionPaymentRepository.save(subscriptionPayment);
      log.info("결제 기록 저장 완료: id={}, merchantUid={}, status={}",
          subscriptionPayment.getId(), merchantUid, subscriptionPayment.getStatus());

    } catch (Exception e) {
      log.error("정기 결제 처리 중 오류 발생: {}", e.getMessage(), e);

      // 결제 실패 기록
      try {
        // 상품 가격 정보 가져오기
        Integer amount = 0; // 기본값
        if (subscription.getProduct() != null && subscription.getProduct().getPrice() != null) {
          amount = subscription.getProduct().getPrice();
        }

        // 결제 방법 확인 (nullable=false)
        PaymentMethod paymentMethod = null;
        try {
          // 1. 최근 결제 내역의 결제 수단 조회 시도
          List<SubscriptionPayment> recentPayments = subscriptionPaymentRepository
              .findTop1BySubscriptionIdAndStatusOrderByCreatedAtDesc(
                  subscription.getId(), PaymentStatus.PAID);

          if (!recentPayments.isEmpty() && recentPayments.get(0).getPaymentMethod() != null) {
            paymentMethod = recentPayments.get(0).getPaymentMethod();
          } else {
            // 2. 사용자의 기본 결제 수단 조회
            paymentMethod = paymentMethodRepository
                .findByUser_UserIdAndIsDefaultTrueAndDeletedAtIsNull(subscription.getUser().getUserId())
                .orElse(null);
          }
        } catch (Exception ex) {
          log.warn("결제 수단 조회 실패: {}", ex.getMessage());
        }

        // 결제 수단을 찾지 못한 경우 실패 기록을 저장할 수 없음
        if (paymentMethod == null) {
          log.error("결제 실패 기록을 저장할 결제 수단을 찾을 수 없습니다: subscriptionId={}", subscription.getId());
          return;
        }

        SubscriptionPayment failedPayment = SubscriptionPayment.builder()
            .subscription(subscription)
            .paymentMethod(paymentMethod)  // 필수 필드
            .status(PaymentStatus.FAILED)
            .amount(amount)  // 필수 필드
            .merchantUid("failed_" + subscription.getId() + "_" + System.currentTimeMillis())
            .pgProvider(paymentMethod.getProvider() != null ? paymentMethod.getProvider() : "UNKNOWN")  // 필수 필드
            .pgResponse(e.getMessage() != null ? e.getMessage() : "결제 처리 중 오류 발생")
            .build();

        subscriptionPaymentRepository.save(failedPayment);
        log.info("결제 실패 기록 저장 완료: subscriptionId={}", subscription.getId());
      } catch (Exception ex) {
        log.error("결제 실패 기록 저장 중 오류: {}", ex.getMessage());
      }
    }
  }

  // 웹훅
  @Override
  @Transactional
  public void handleWebhook(WebhookRequest request) {
    log.info("웹훅 수신: impUid={}, merchantUid={}, status={}",
        request.getImp_uid(), request.getMerchant_uid(), request.getStatus());

    try {
      // 결제 정보 조회 및 검증
      JsonNode paymentData = portoneClient.getPaymentData(request.getImp_uid());

      // 결제 검증
      if (paymentData == null) {
        log.error("웹훅 처리 실패: 유효하지 않은 결제 정보");
        return;
      }

      // 웹훅 데이터와 실제 결제 정보 검증
      String merchantUid = paymentData.path("merchant_uid").asText();
      if (!merchantUid.equals(request.getMerchant_uid())) {
        log.error("웹훅 처리 실패: 주문번호 불일치 webhook={}, actual={}",
            request.getMerchant_uid(), merchantUid);
        return;
      }

      // merchantUid로 기존 결제 내역 조회
      Optional<SubscriptionPayment> optionalPayment = subscriptionPaymentRepository.findByMerchantUid(request.getMerchant_uid());

      if (optionalPayment.isPresent()) {
        SubscriptionPayment subscriptionPayment = optionalPayment.get();

        // 결제 상태 업데이트
        String status = paymentData.path("status").asText();
        switch (status) {
          case "paid":
            // 이미 처리된 결제인지 확인
            if ("PAID".equals(subscriptionPayment.getStatus().name())) {
              log.info("이미 처리된 결제입니다: {}", request.getMerchant_uid());
              return;
            }

            subscriptionPayment.setStatus(PaymentStatus.PAID);
            // JSON에서 timestamp를 가져오고 변환
            long paidAtTimestamp = paymentData.path("paid_at").asLong(); // 초 단위
            LocalDateTime paidAtDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(paidAtTimestamp),
                ZoneId.systemDefault());
            subscriptionPayment.setPaidAt(paidAtDateTime);

            // 다음 결제일 업데이트
            updateNextPaymentDateAfterWebhook(subscriptionPayment.getSubscription().getId());
            break;

          case "failed":
            subscriptionPayment.setStatus(PaymentStatus.FAILED);
            subscriptionPayment.setPgResponse("결제 실패: " + paymentData.path("fail_reason").asText());
            break;

          case "cancelled":
            subscriptionPayment.setStatus(PaymentStatus.CANCELED);
            break;
        }

        // 응답 데이터 업데이트
        subscriptionPayment.setPgResponse(objectMapper.writeValueAsString(paymentData));

        subscriptionPaymentRepository.save(subscriptionPayment);
        log.info("웹훅 처리 완료: merchantUid={}, 상태={}", request.getMerchant_uid(), status);
      } else {
        // 결제 내역이 없는 경우 새로 생성 (웹훅이 먼저 도착한 경우)
        // 주문번호 형식에서 구독 ID 추출 (예: recurring_123_timestamp)
        String webHookMerchantUid  = request.getMerchant_uid();
        if (merchantUid.startsWith("recurring_")) {
          try {
            String[] parts = merchantUid.split("_");
            if (parts.length >= 2) {
              Long subscriptionId = Long.parseLong(parts[1]);
              Subscription subscription = subscriptionRepository.findById(subscriptionId)
                  .orElse(null);

              if (subscription != null) {
                // 결제 내역 생성
                SubscriptionPayment newSubscriptionPayment = new SubscriptionPayment();
                newSubscriptionPayment.getSubscription().setId(subscriptionId);
                newSubscriptionPayment.setMerchantUid(merchantUid);
                newSubscriptionPayment.setAmount(paymentData.path("amount").asInt());

                // 결제 수단 설정
                PaymentMethod paymentMethod = paymentMethodRepository
                    .findByUser_UserIdAndIsDefaultTrueAndDeletedAtIsNull(subscription.getUser().getUserId())
                    .orElse(null);

                if (paymentMethod != null) {
                  newSubscriptionPayment.getPaymentMethod().setId(paymentMethod.getId());
                }

                // 상태 설정
                String paymentStatus = paymentData.path("status").asText();
                if ("paid".equals(paymentStatus)) {
                  newSubscriptionPayment.setStatus(PaymentStatus.PAID);
                  long paidAtTimestamp = paymentData.path("paid_at").asLong(); // 초 단위
                  LocalDateTime paidAtDateTime = LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(paidAtTimestamp),
                      ZoneId.systemDefault());
                  newSubscriptionPayment.setPaidAt(paidAtDateTime);


                  // 다음 결제일 업데이트
                  updateNextPaymentDateAfterWebhook(subscriptionId);
                } else {
                  newSubscriptionPayment.setStatus(PaymentStatus.FAILED);
                  newSubscriptionPayment.setPgResponse("결제 실패: " + paymentData.path("fail_reason").asText());
                }

                newSubscriptionPayment.setPgProvider("KAKAO");
                newSubscriptionPayment.setPgResponse(objectMapper.writeValueAsString(paymentData));

                subscriptionPaymentRepository.save(newSubscriptionPayment);
                log.info("웹훅으로 새 결제 내역 생성: merchantUid={}, 상태={}",
                    merchantUid, newSubscriptionPayment.getStatus());
              }
            }
          } catch (Exception e) {
            log.error("웹훅 처리 중 구독 ID 추출 오류: {}", e.getMessage());
          }
        } else {
          log.warn("웹훅 처리: 해당 merchantUid에 대한 결제 내역이 없습니다: {}", merchantUid);
        }
      }
    } catch (Exception e) {
      log.error("웹훅 처리 중 오류 발생: {}", e.getMessage());
    }
  }

  private void updateNextPaymentDateAfterWebhook(Long subscriptionId) {
    try {
      Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
      if (subscription != null && "ACTIVE".equals(subscription.getStatus().name()) && Boolean.TRUE.equals(subscription.getAutoRenew())) {
        Product product = productRepository.findById(subscription.getProduct().getId()).orElse(null);
        if (product != null) {
          LocalDateTime nextPaymentDateTime =
              calculateNextPaymentDate(LocalDateTime.now(), product.getPeriod());

          // 월말 처리 (예: 1월 31일 -> 2월 28일)
          int currentDay = LocalDateTime.now().getDayOfMonth();
          Month nextMonth = nextPaymentDateTime.getMonth();
          int daysInNextMonth = nextMonth.length(Year.isLeap(nextPaymentDateTime.getYear()));

          if (currentDay > daysInNextMonth) {
            // 다음 달의 마지막 날로 조정
            nextPaymentDateTime = nextPaymentDateTime.withDayOfMonth(daysInNextMonth);
          }
          subscription.setNextPaymentDate(nextPaymentDateTime);
          subscriptionRepository.save(subscription);

          log.info("웹훅 이후 다음 결제일 업데이트: subscriptionId={}, nextPaymentDate={}",
              subscription.getId(), nextPaymentDateTime);
        }
      }
    } catch (Exception e) {
      log.error("다음 결제일 업데이트 중 오류 발생: {}", e.getMessage());
    }
  }

  // 유틸리티 메서드
  private LocalDateTime calculateNextPaymentDate(LocalDateTime currentDate, ProductPeriod period) {
    return switch (period.name()) {
      case "MONTH" -> currentDate.plusMonths(1);
      case "YEAR" -> currentDate.plusYears(1);
      default -> currentDate.plusMonths(1); // 기본값은 1개월
    };
  }

  private BillingKeyResponse.KakaoPayMethodRes convertToKakaoPayMethodRes(PaymentMethod entity) {
    String provider = entity.getProvider();
    String cardNumber = entity.getCardNumber();

    // 카카오페이 제공사인 경우 특별 처리
    if ("KAKAO".equals(provider)) {
      try {
        // 메타데이터에서 추가 정보 추출 (있는 경우)
        if (entity.getMetaData() != null && !entity.getMetaData().isEmpty()) {
          ObjectMapper mapper = new ObjectMapper();
          Map<String, Object> metaData = mapper.readValue(entity.getMetaData(), Map.class);

          // 카드 브랜드 정보가 있으면 provider에 추가
          if (metaData.containsKey("card_name") || metaData.containsKey("card_brand")) {
            String cardName = (String) metaData.getOrDefault("card_name", "");
            String cardBrand = (String) metaData.getOrDefault("card_brand", "");

            if (!cardName.isEmpty() || !cardBrand.isEmpty()) {
              provider = "KAKAO (" + (!cardName.isEmpty() ? cardName : cardBrand) + ")";
            }
          }
        }
      } catch (Exception e) {
        log.warn("카카오페이 메타데이터 파싱 중 오류 발생: {}", e.getMessage());
      }
    }

    return BillingKeyResponse.KakaoPayMethodRes.builder()
        .id(entity.getId())
        .type(entity.getType())
        .provider(provider)
        .cardNumber(cardNumber)
        .isDefault(entity.getIsDefault())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  private String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 8) {
      return cardNumber;
    }

    // 앞 6자리와 뒤 4자리만 남기고 마스킹
    String prefix = cardNumber.substring(0, 6);
    String suffix = cardNumber.substring(cardNumber.length() - 4);

    return prefix + "******" + suffix;
  }
}