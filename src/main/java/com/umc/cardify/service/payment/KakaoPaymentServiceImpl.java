package com.umc.cardify.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.*;
import com.umc.cardify.domain.enums.BillingKeyStatus;
import com.umc.cardify.domain.enums.PaymentStatus;
import com.umc.cardify.domain.enums.PaymentType;
import com.umc.cardify.dto.payment.billing.BillingKeyRequestDTO;
import com.umc.cardify.dto.payment.billing.BillingKeyResponse;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

      requestData.put("m_redirect_url", baseUrl + "/api/v1/payments/kakaopay/mobile-success"); // 모바일 환경
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

  // 빌링키 검증
  @Override
  @Transactional
  public BillingKeyResponse.VerifyBillingKeyRes verifyAndSaveBillingKey(BillingKeyRequestDTO.VerifyBillingKeyReq request) {
    log.info("빌링키 검증 시작: customerUid={}, userId={}", request.customerUid(), request.userId());

    try {
      // 빌링키 유효성 검증
      JsonNode billingKeyResponse = portoneClient.getBillingKey(request.customerUid());

      if (billingKeyResponse == null) {
        log.error("빌링키 검증 실패: 응답이 없습니다");
        throw new RuntimeException("빌링키 검증 실패: 응답이 없습니다");
      }

      // 추가 검증: 빌링키 상태 확인
      String status = billingKeyResponse.path("status").asText();
      if (!"active".equals(status)) {
        log.error("빌링키 상태 오류: status={}", status);
        throw new RuntimeException("유효하지 않은 빌링키 상태: " + status);
      }

      // 카드 정보 JSON 파싱
      Map<String, Object> cardInfo = objectMapper.readValue(request.cardInfo(), Map.class);

      // 기존 결제 수단이 있는지 확인하고 default 상태 변경
      List<PaymentMethod> existingMethods = paymentMethodRepository.findByUser_UserIdAndDeletedAtIsNull(request.userId());

      if (!existingMethods.isEmpty()) {
        for (PaymentMethod method : existingMethods) {
          method.setIsDefault(false);
        }
        paymentMethodRepository.saveAll(existingMethods);
      }

      // 사용자 조회
      User user = userRepository.findById(request.userId())
          .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.userId()));

      // 새 결제 수단 생성
      PaymentMethod paymentMethod = new PaymentMethod();
      paymentMethod.setUser(user);
      paymentMethod.setType(PaymentType.CARD);
      paymentMethod.setProvider(PaymentType.KAKAO.name());

      // 빌링키 저장
      paymentMethod.setBillingKey(request.customerUid());

      // 카드 정보 마스킹 처리
      if (cardInfo.containsKey("card_number")) {
        String maskedCardNumber = maskCardNumber((String) cardInfo.get("card_number"));
        paymentMethod.setCardNumber(maskedCardNumber);
        cardInfo.put("card_number", maskedCardNumber); // 마스킹된 번호로 교체
      }

      // 메타데이터에 카드 상세 정보 저장
      paymentMethod.setMetaData(objectMapper.writeValueAsString(cardInfo));

      // 유효기간 설정 (있는 경우)
      if (cardInfo.containsKey("expiry")) {
        String expiry = (String) cardInfo.get("expiry");
        if (expiry != null && expiry.length() >= 4) {
          // YYMM 형식 가정
          int year = Integer.parseInt(expiry.substring(0, 2)) + 2000;
          int month = Integer.parseInt(expiry.substring(2, 4));

          Calendar calendar = Calendar.getInstance();
          calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
          paymentMethod.setValidUntil(LocalDateTime.ofInstant(
              calendar.toInstant(), ZoneId.systemDefault()).toLocalDate());
        }
      }

      // 기본 결제 수단으로 설정
      paymentMethod.setIsDefault(true);

      // 저장
      PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
      log.info("결제 수단 저장 완료: id={}", savedPaymentMethod.getId());

      // 구독 생성
      subscriptionServiceImpl.createSubscription(SubscriptionRequest.CreateSubscriptionReq.builder()
          .userId(request.userId())
          .productId(request.productId())
          .paymentMethodId(savedPaymentMethod.getId())
          .autoRenew(true)
          .build(), null);

      return BillingKeyResponse.VerifyBillingKeyRes.builder()
          .id(savedPaymentMethod.getId())
          .userId(savedPaymentMethod.getUser().getUserId())
          .type(savedPaymentMethod.getType().name())
          .provider(savedPaymentMethod.getProvider())
          .isDefault(savedPaymentMethod.getIsDefault())
          .build();
    } catch (IOException e) {
      log.error("데이터 처리 오류: {}", e.getMessage());
      throw new RuntimeException("데이터 처리 오류: " + e.getMessage());
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

      // 3. pg_token 저장 및 상태 업데이트
      billingKeyRequest.setPgToken(request.pgToken());
      billingKeyRequest.updateStatus(BillingKeyStatus.APPROVED);

      // 4. 결제 수단 정보 저장
      User user = billingKeyRequest.getUser();

      // 기존 결제 수단이 있는지 확인하고 default 상태 변경
      List<PaymentMethod> existingMethods = paymentMethodRepository.findByUser_UserIdAndDeletedAtIsNull(user.getUserId());
      if (!existingMethods.isEmpty()) {
        for (PaymentMethod method : existingMethods) {
          method.setIsDefault(false);
        }
        paymentMethodRepository.saveAll(existingMethods);
      }

      // 새 결제 수단 저장
      PaymentMethod paymentMethod = PaymentMethod.builder()
          .user(user)
          .type(PaymentType.CARD)
          .provider("KAKAOPAY")
          .billingKey(billingKeyRequest.getCustomerUid())
          .isDefault(true)
          .validUntil(LocalDate.now().plusYears(1))
          .build();

      PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);

      // 빌링키 요청과 결제 수단 연결
      billingKeyRequest.setPaymentMethod(savedPaymentMethod);

      // 5. 구독 생성 (상품이 선택된 경우)
      Long subscriptionId = null;
      if (billingKeyRequest.getProduct() != null) {
        // 구독 생성 로직 (별도 서비스 메서드 호출)
        // subscriptionId = subscriptionServiceImpl.createSubscription(...);
      }

      // 6. 응답 구성
      return BillingKeyResponse.ApproveBillingKeyRes.builder()
          .merchantUid(request.merchantUid())
          .customerUid(billingKeyRequest.getCustomerUid())
          .status("success")
          .paymentMethodId(savedPaymentMethod.getId())
          .userId(user.getUserId())
          .subscriptionId(subscriptionId)
          .build();
    } catch (Exception e) {
      log.error("빌링키 발급 승인 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("빌링키 발급 승인 중 오류 발생: " + e.getMessage(), e);
    }
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

  // 정기 결제
  @Override
  @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시에 실행
  @Transactional
  public void processRecurringPayments() {
    log.info("정기 결제 처리 시작");

    // 오늘 결제해야 할 구독 목록 조회
    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

    List<Subscription> subscriptionsDue = subscriptionRepository.findByStatusAndAutoRenewTrueAndNextPaymentDateBetween(
        "ACTIVE",
        Timestamp.valueOf(startOfDay),
        Timestamp.valueOf(endOfDay)
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
      Product product = productRepository.findById(subscription.getProduct().getId())
          .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다: " + subscription.getProduct().getId()));

      // 결제 수단 조회
      PaymentMethod paymentMethod = paymentMethodRepository.findByUser_UserIdAndIsDefaultTrueAndDeletedAtIsNull(subscription.getUser().getUserId())
          .orElseThrow(() -> new RuntimeException("기본 결제 수단을 찾을 수 없습니다: " + subscription.getUser().getUserId()));

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
      SubscriptionPayment subscriptionPayment = new SubscriptionPayment();
      subscriptionPayment.getSubscription().setId(subscription.getId());
      subscriptionPayment.getPaymentMethod().setId(paymentMethod.getId());
      subscriptionPayment.setMerchantUid(merchantUid);
      subscriptionPayment.setAmount(product.getPrice());

      // 결제 상태 설정
      if ("paid".equals(paymentStatus)) {
        subscriptionPayment.setStatus(PaymentStatus.PAID);
        subscriptionPayment.setPaidAt(LocalDateTime.now());
      } else {
        subscriptionPayment.setStatus(PaymentStatus.FAILED);
        subscriptionPayment.setPgResponse("결제 상태 확인 실패: " + paymentStatus);
      }

      subscriptionPayment.setPgProvider("KAKAO");

      // 응답 데이터 저장
      subscriptionPayment.setPgResponse(objectMapper.writeValueAsString(response));

      SubscriptionPayment savedPayment = subscriptionPaymentRepository.save(subscriptionPayment);
      log.info("결제 기록 저장 완료: id={}, merchantUid={}, status={}",
          savedPayment.getId(), merchantUid, subscriptionPayment.getStatus());

      // 결제 성공 시 다음 결제일 업데이트
      if ("PAID".equals(subscriptionPayment.getStatus().name())) {
        LocalDateTime nextPaymentDate = calculateNextPaymentDate(
            subscription.getNextPaymentDate(),
            product.getPeriod()
        );
        subscription.setNextPaymentDate(nextPaymentDate);
        subscriptionRepository.save(subscription);

        log.info("다음 결제일 업데이트: subscriptionId={}, nextPaymentDate={}",
            subscription.getId(), nextPaymentDate);
      }

    } catch (Exception e) {
      log.error("정기 결제 처리 중 오류 발생: {}", e.getMessage());

      // 결제 실패 기록
      SubscriptionPayment failedPayment = new SubscriptionPayment();
      failedPayment.getSubscription().setId(subscription.getId());
      failedPayment.setStatus(PaymentStatus.FAILED);
      failedPayment.setPgResponse(e.getMessage());
      failedPayment.setMerchantUid("failed_" + subscription.getId() + "_" + System.currentTimeMillis());

      subscriptionPaymentRepository.save(failedPayment);
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
          LocalDateTime nextPaymentDate = calculateNextPaymentDate(
              subscription.getNextPaymentDate(),
              product.getPeriod()
          );
          subscription.setNextPaymentDate(nextPaymentDate);
          subscriptionRepository.save(subscription);

          log.info("웹훅 이후 다음 결제일 업데이트: subscriptionId={}, nextPaymentDate={}",
              subscription.getId(), nextPaymentDate);
        }
      }
    } catch (Exception e) {
      log.error("다음 결제일 업데이트 중 오류 발생: {}", e.getMessage());
    }
  }

  // 유틸리티 메서드
  private LocalDateTime calculateNextPaymentDate(LocalDateTime currentDate, String period) {
    return switch (period) {
      case "MONTHLY" -> currentDate.plusMonths(1);
      case "YEARLY" -> currentDate.plusYears(1);
      case "WEEKLY" -> currentDate.plusWeeks(1);
      case "DAILY" -> currentDate.plusDays(1);
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