package com.umc.cardify.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import com.umc.cardify.config.exception.PaymentFailedException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimplePayServiceImpl implements SimplePayService {

  private final IamportClient iamportClient;
  private final PortoneClient portoneClient;
  private final PaymentMethodRepository paymentMethodRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final SubscriptionServiceImpl subscriptionServiceImpl;
  private final BillingKeyRequestRepository billingKeyRequestRepository;

  @Value("${portone.kakaopay_pg_code}")
  private String KAKAOPAY_PG_CODE;

  @Value("${portone.tosspay_pg_code}")
  private String TOSSPAY_PG_CODE;

  @Value("${portone.naverpay_pg_code}")
  private String NAVERPAY_PG_CODE;

  @Value("${tosspay.api.channel_key}")
  private String TOSS_CHANNEL_KEY;

  // 빌링키 요청
  @Override
  @Transactional
  public BillingKeyResponse.RequestBillingKeyRes requestBillingKey(BillingKeyRequestDTO.RequestBillingKeyReq request) {
    log.info("빌링키 요청 시작: userId={}, productId={}, pgProvider={}",
        request.userId(), request.productId(), request.pgProvider());

    // PG사 코드 결정
    String pgCode = getPgCode(request.pgProvider());

    // 구독 중복 확인
    long activeSubscriptionCount = subscriptionRepository.countByUser_UserIdAndStatus(request.userId(), SubscriptionStatus.ACTIVE);

    if (activeSubscriptionCount > 0) {
      List<BillingKeyRequest> billingKeyRequestList = billingKeyRequestRepository.findByUserUserIdAndStatus(request.userId(), BillingKeyStatus.REQUESTED);
      if (!billingKeyRequestList.isEmpty()) {
        BillingKeyRequest billingKeyRequest = billingKeyRequestList.get(billingKeyRequestList.size() - 1);
        billingKeyRequest.setStatus(BillingKeyStatus.FAILED);
      }
      throw new IllegalStateException("이미 활성 상태의 구독이 있습니다. 새로운 구독을 시작하기 전에 기존 구독을 취소해 주세요.");
    }

    // 고유 식별자 생성
    String merchantUid = "subscribe_" + UUID.randomUUID();
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

      // 콜백 URL 설정
      String baseUrl = request.callbackUrl() != null ? request.callbackUrl() : "http://localhost:8080";
      String successUrl = baseUrl + "/api/v1/payments/simple-pay/success";
      String failUrl = baseUrl + "/api/v1/payments/simple-pay/fail";

      // PG사별 빌링키 요청 처리
      Map<String, Object> requestData;

      // pg사 별 빌링키 발급 요청
      if (TOSSPAY_PG_CODE.equals(pgCode)) {
       requestData = requestTosspayBillingKey(
            customerUid,
            request.name(),
            request.email(),
            merchantUid,
            successUrl
       );

      } else if (KAKAOPAY_PG_CODE.equals(pgCode)) {
        requestData = requestKakaopayBillingKey(
            pgCode,
            customerUid,
            request.name(),
            request.email(),
            merchantUid,
            successUrl,
            failUrl
        );
      } else {
        // TODO : 이후 수정
        throw new RuntimeException("지원하지 않는 PG사입니다: " + pgCode);
      }
      // 빌링키 요청 정보 저장
      BillingKeyRequest billingKeyRequest = BillingKeyRequest.builder()
          .user(user)
          .merchantUid(merchantUid)
          .customerUid(customerUid)
          .status(BillingKeyStatus.REQUESTED)
          .pgProvider(pgCode)
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

  // 카카오페이 빌링키 발급
  private Map<String, Object> requestKakaopayBillingKey(String pgCode, String customerUid, String customerName, String customerEmail,
                                                       String merchantUid, String successUrl, String failUrl) {
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("pg", pgCode);
    requestData.put("pay_method", "card");
    requestData.put("merchant_uid", merchantUid);
    requestData.put("customer_uid", customerUid);
    requestData.put("name", "구독 서비스 자동결제 등록");
    requestData.put("amount", 0); // 빌링키 발급은 0원
    requestData.put("buyer_email", customerEmail);
    requestData.put("buyer_name", customerName);
    // TODO : 모바일 버전 확인 후 수정
    //requestData.put("m_redirect_url", baseUrl + "/api/v1/payments/mobile-success");
    requestData.put("success_url", successUrl);
    requestData.put("fail_url", failUrl);

    return requestData;
  }

  // 토스페이 빌링키 발급 요청
  private Map<String, Object> requestTosspayBillingKey(String customerUid, String customerName, String customerEmail,
                                                       String merchantUid, String successUrl) {

    // 토스페이 V1 빌링키 발급 요청 데이터 구성
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("channelKey", TOSS_CHANNEL_KEY);
    requestData.put("pg", TOSSPAY_PG_CODE);
    requestData.put("pay_method", "tosspay");
    requestData.put("merchant_uid", merchantUid);
    requestData.put("name", "토스페이 정기 결제 빌링키 발급");
    requestData.put("customer_id", customerUid); // 필수
    requestData.put("customer_uid", customerUid); // 필수
    requestData.put("buyer_email", customerEmail);
    requestData.put("buyer_name", customerName);

    // 모바일 환경을 위한 리디렉션 URL
    requestData.put("m_redirect_url", successUrl);
    requestData.put("notice_url", successUrl);

    return requestData;
  }

  // 빌링키 발급 승인
  @Override
  @Transactional
  public BillingKeyResponse.ApproveBillingKeyRes approveBillingKey(BillingKeyRequestDTO.ApproveBillingKeyReq request) {
    log.info("빌링키 발급 승인 시작: pgToken={}, merchantUid={}", request.pgToken(), request.merchantUid());

    try {
      // 1. 빌링키 요청 정보 조회
      BillingKeyRequest billingKeyRequest = billingKeyRequestRepository.findByMerchantUid(request.merchantUid())
          .orElseThrow(() -> new RuntimeException("빌링키 요청 정보를 찾을 수 없습니다: " + request.merchantUid()));

      // PG사 코드 확인
      String pgCode = billingKeyRequest.getPgProvider();
      PaymentType paymentType = getPaymentType(pgCode);

      // 2. 포트원 API 호출하여 결제 정보 조회
      Payment payment = null;
      String cardCompany = null;
      String cardNumber = null;

      // PG사별 결제 정보 조회
      if (TOSSPAY_PG_CODE.equals(pgCode)) {
        // 토스페이는 imp_uid(tid) 사용
        IamportResponse<Payment> paymentResponse = iamportClient.paymentByImpUid(request.tid());
        payment = paymentResponse.getResponse();

        if (payment != null) {
          cardCompany = payment.getCardName();
          cardNumber = payment.getCardNumber();
        }
      } else if (KAKAOPAY_PG_CODE.equals(pgCode)) {
        // 카카오페이는 별도 검증 로직 필요 없음 (프론트에서 처리)
        // 필요시 카카오페이 결제 정보 조회 API 호출
      }

      // 결제 정보 검증
      if (payment == null && TOSSPAY_PG_CODE.equals(pgCode)) {
        billingKeyRequest.updateStatus(BillingKeyStatus.FAILED);
        throw new RuntimeException("결제 정보를 찾을 수 없습니다: " + request.tid());
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
          .type(paymentType)
          .provider(getProviderName(pgCode))
          .billingKey(billingKeyRequest.getCustomerUid())
          .cardNumber(cardNumber)
          .isDefault(true) // 결제한 수단을 기본 결제 수단으로
          .build();

      PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
      log.info("결제 수단 저장 완료: id={}, billingKey={}", savedPaymentMethod.getId(), maskString(billingKeyRequest.getCustomerUid()));

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

      // 구독 생성
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

    } catch (IamportResponseException | IOException e) {
      log.error("빌링키 발급 승인 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("빌링키 발급 승인 중 오류 발생: " + e.getMessage(), e);
    }
  }

  // 빌링키 상태 조회
  @Override
  public BillingKeyResponse.BillingStatusRes getBillingStatus(String merchantUid) {
    log.info("빌링키 상태 조회: merchantUid={}", merchantUid);

    BillingKeyRequest billingKeyRequest = billingKeyRequestRepository.findByMerchantUid(merchantUid)
        .orElseThrow(() -> new RuntimeException("빌링키 요청 정보를 찾을 수 없습니다: " + merchantUid));

    return BillingKeyResponse.BillingStatusRes.builder()
        .merchantUid(billingKeyRequest.getMerchantUid())
        .customerUid(billingKeyRequest.getCustomerUid())
        .status(billingKeyRequest.getStatus().name())
        .build();
  }

  // 결제 취소
  @Override
  @Transactional
  public boolean cancelPayment(SubscriptionRequest.CancelPaymentReq request) {
    log.info("결제 취소 요청: subscriptionId={}", request.merchantUid());

    // 결제 조회
    SubscriptionPayment subscriptionPayment = subscriptionPaymentRepository.findByMerchantUid(request.merchantUid())
        .orElseThrow(() -> new ResourceNotFoundException("결제 내역을 찾을 수 없습니다: " + request.merchantUid()));

    // 이미 취소된 결제인지 확인
    if ("CANCELLED".equals(subscriptionPayment.getStatus().name())) {
      log.warn("이미 취소된 결제입니다: merchantUid={}", request.merchantUid());
      return false;
    }

    // 포트원 API를 통해 결제 취소
    CancelData cancelData = cancelData(subscriptionPayment.getMerchantUid(), "사용자 요청에 의한 취소");

    // 결제 상태 업데이트
    subscriptionPayment.setStatus(PaymentStatus.CANCELED);
    subscriptionPaymentRepository.save(subscriptionPayment);

    // 구독 상태 업데이트
    Subscription subscription = subscriptionPayment.getSubscription();
    subscription.setStatus(SubscriptionStatus.CANCELLED);
    subscription.setEndDate(LocalDateTime.now());
    subscription.setAutoRenew(false);
    subscriptionRepository.save(subscription);

    log.info("결제 취소 완료: subscriptionId={}, paymentId={}", subscription.getId(), subscriptionPayment.getId());
    return true;

  }

  // 정기 결제 처리
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
        // 지수 백오프 방식으로 결제 처리 시도
        boolean success = processSubscriptionPaymentWithBackoff(subscription);

        if (!success) {
          log.error("구독 ID {}의 정기 결제가 모든 시도 후에도 실패했습니다.", subscription.getId());
          // TODO : 결제 실패 알림 기능 구현
        }
      } catch (Exception e) {
        log.error("구독 ID {}의 정기 결제 처리 자체에 심각한 오류: {}", subscription.getId(), e.getMessage(), e);
        // TODO : 결제 실패 알림 기능 구현
      }
    }

    log.info("정기 결제 처리 완료");
  }

  // 지수 백오프 정기 결제 처리
  private boolean processSubscriptionPaymentWithBackoff(Subscription subscription) {
    int attempt = 0;
    final int maxRetries = 10; // 최대 시도 횟수

    while (attempt <= maxRetries) {
      try {
        // 정기 결제 처리 시도
        processSubscriptionPayment(subscription);
        log.info("구독 ID {}의 정기 결제 성공 (시도 {}/{})",
            subscription.getId(), attempt + 1, maxRetries);
        return true; // 성공
      } catch (Exception e) {
        attempt++;
        log.error("구독 ID {}의 정기 결제 처리 중 오류 (시도 {}/{}): {}",
            subscription.getId(), attempt, maxRetries, e.getMessage(), e);

        // 최대 재시도 횟수에 도달했는지 확인
        if (attempt >= maxRetries) {
          log.warn("구독 ID {}의 정기 결제 최대 재시도 횟수({}) 초과",
              subscription.getId(), maxRetries);
          break;
        }

        // 지수 백오프 계산
        long backoffTime = (long) Math.pow(2, attempt) * 1000;

        log.info("구독 ID {}의 정기 결제 재시도 예정: {}ms 후 시도 {}/{}",
            subscription.getId(), backoffTime, attempt + 1, maxRetries);

        try {
          Thread.sleep(backoffTime);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    }

    return false; // 모든 재시도 실패
  }

  // 구독 처리
  private void processSubscriptionPayment(Subscription subscription) {
    log.info("구독 ID {}의 결제 처리 시작", subscription.getId());

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

    try {

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
      parameters.put("pg", getPgCode(paymentMethod.getProvider()));

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

        // 월말 처리 (예: 1월 31일 -> 2월 28일)
        int currentDay = currentNextPaymentDate.getDayOfMonth();
        Month nextMonth = nextPaymentDateTime.getMonth();
        int daysInNextMonth = nextMonth.length(Year.isLeap(nextPaymentDateTime.getYear()));

        if (currentDay > daysInNextMonth) {
          // 다음 달의 마지막 날로 조정
          nextPaymentDateTime = nextPaymentDateTime.withDayOfMonth(daysInNextMonth);
        }

        subscription.setNextPaymentDate(nextPaymentDateTime);

        // 결제시 구독 종료일 자동 갱신
        String period = subscription.getProduct().getPeriod().name();
        LocalDateTime newEndDate = ("YEAR".equals(period)) ?
            subscription.getEndDate().plusYears(1) : subscription.getEndDate().plusMonths(1);

        subscription.setEndDate(newEndDate);
        log.info("구독 기간 연장: id={}, 새 종료일={}", subscription.getId(), newEndDate);

        subscriptionRepository.save(subscription);
        log.info(" 다음 결제일 업데이트: subscriptionId={}, nextPaymentDate={}",
            subscription.getId(), nextPaymentDateTime);
      } else {
        subscriptionPayment.setStatus(PaymentStatus.FAILED);
        // TODO : 결제 실패 알림 기능 구현
      }

      subscriptionPaymentRepository.save(subscriptionPayment);
      log.info("결제 기록 저장 완료: id={}, merchantUid={}, status={}",
          subscriptionPayment.getId(), merchantUid, subscriptionPayment.getStatus());

    } catch (Exception e) {
      log.error("정기 결제 처리 중 오류 발생: {}", e.getMessage(), e);

      // 결제 실패 기록
      SubscriptionPayment failedPayment = SubscriptionPayment.builder()
          .subscription(subscription)
          .paymentMethod(paymentMethod)  // 필수 필드
          .status(PaymentStatus.FAILED)
          .amount(subscription.getProduct().getPrice())  // 필수 필드
          .merchantUid("failed_" + subscription.getId() + "_" + System.currentTimeMillis())
          .pgProvider(paymentMethod.getProvider() != null ? paymentMethod.getProvider() : "UNKNOWN")  // 필수 필드
          .pgResponse(e.getMessage() != null ? e.getMessage() : "결제 처리 중 오류 발생")
          .build();
      subscriptionPaymentRepository.save(failedPayment);
      log.info("결제 실패 기록 저장 완료: subscriptionId={}", subscription.getId());

      // 중요: 예외를 다시 던져서 백오프 메서드가 이를 감지할 수 있게 함
      if (e instanceof PaymentFailedException) {
        throw (PaymentFailedException) e;
      } else {
        throw new PaymentFailedException("결제 처리 중 오류 발생: " + e.getMessage(), e);
      }
    }
  }

  // 웹훅
  @Override
  @Transactional
  public void handleWebhook(WebhookRequest request) {
    log.info("웹훅 수신: imp_uid={}, merchant_uid={}, status={}", request.getImp_uid(), request.getMerchant_uid(), request.getStatus());

    try {
      // imp_uid로 결제 정보 조회
      IamportResponse<Payment> response = iamportClient.paymentByImpUid(request.getImp_uid());
      Payment payment = response.getResponse();

      if (payment == null) {
        log.warn("웹훅에 해당하는 결제 정보를 찾을 수 없습니다: {}", request.getImp_uid());
        return;
      }

      String merchantUid = payment.getMerchantUid();

      // merchant_uid 접두어로 처리 분기
      if (merchantUid.startsWith("subscribe_")) {
        // 빌링키 발급 관련 웹훅
        handleBillingKeyWebhook(merchantUid, payment, request.getStatus());
      } else if (merchantUid.startsWith("recurring_")) {
        // 정기 결제 관련 웹훅
        handleRecurringPaymentWebhook(merchantUid, payment, request.getStatus());
      } else {
        log.warn("알 수 없는 형식의 merchant_uid: {}", merchantUid);
      }

    } catch (IamportResponseException | IOException e) {
      log.error("웹훅 처리 중 오류: {}", e.getMessage(), e);
      throw new RuntimeException("웹훅 처리 중 오류 발생: " + e.getMessage(), e);
    }
  }

  // 빌링키 발급 웹훅 처리
  private void handleBillingKeyWebhook(String merchantUid, Payment payment, String status) {
    BillingKeyRequest billingKeyRequest = billingKeyRequestRepository.findByMerchantUid(merchantUid)
        .orElse(null);

    if (billingKeyRequest == null) {
      log.warn("웹훅에 해당하는 빌링키 요청을 찾을 수 없습니다: {}", merchantUid);
      return;
    }

    if ("paid".equals(status)) {
      billingKeyRequest.updateStatus(BillingKeyStatus.APPROVED);
      log.info("빌링키 발급 성공 처리: {}", merchantUid);
    } else if ("failed".equals(status)) {
      billingKeyRequest.updateStatus(BillingKeyStatus.FAILED);
      log.warn("빌링키 발급 실패 처리: {}", merchantUid);
    }
  }

  // 정기 결제 웹훅 처리
  private void handleRecurringPaymentWebhook(String merchantUid, Payment payment, String status) {
    SubscriptionPayment subscriptionPayment = subscriptionPaymentRepository.findByMerchantUid(merchantUid)
        .orElse(null);

    if (subscriptionPayment == null) {
      log.warn("웹훅에 해당하는 결제 내역을 찾을 수 없습니다: {}", merchantUid);
      return;
    }

    if ("paid".equals(status)) {
      subscriptionPayment.setStatus(PaymentStatus.PAID);
      subscriptionPayment.setPaidAt(LocalDateTime.now());
      log.info("결제 성공 처리: {}", merchantUid);

      // 다음 결제일 별도 메소드로 업데이트 (에러 핸들링 포함)
      updateNextPaymentDateAfterWebhook(subscriptionPayment.getSubscription().getId());

    } else if ("failed".equals(status)) {
      subscriptionPayment.setStatus(PaymentStatus.FAILED);
      log.warn("결제 실패 처리: {}", merchantUid);

    } else if ("cancelled".equals(status)) {
      subscriptionPayment.setStatus(PaymentStatus.CANCELED);
      log.info("결제 취소 처리: {}", merchantUid);

      // 구독 취소 로직 추가 가능
      Subscription subscription = subscriptionPayment.getSubscription();
      subscription.setStatus(SubscriptionStatus.CANCELLED);
      subscription.setAutoRenew(false);
      subscription.setEndDate(LocalDateTime.now());
      subscriptionRepository.save(subscription);
    }

    // 결제 정보 업데이트
    subscriptionPayment.setPgResponse(payment.getStatus());
    subscriptionPaymentRepository.save(subscriptionPayment);
  }

  private CancelData cancelData(String merchantUid, String reason) {
    // merchantUid를 사용하여 CancelData 생성 (두 번째 파라미터 false는 merchant_uid 사용을 의미)
    CancelData cancelData = new CancelData(merchantUid, false);
    cancelData.setReason(reason);
    return cancelData;
  }

  // pg사 이름 변환
  private String getProviderName(String pgCode) {
    if (pgCode.equals(KAKAOPAY_PG_CODE)) {
      return "KAKAOPAY";
    } else if (pgCode.equals(TOSSPAY_PG_CODE)) {
      return "TOSSPAY";
    } else if (pgCode.equals(NAVERPAY_PG_CODE)) {
      return "NAVERPAY";
    } else {
      return "CARD";
    }
  }
  
  // PG사 코드 변환
  private String getPgCode(String pgProvider) {
    return switch (pgProvider.toUpperCase()) {
      case "KAKAO", "KAKAOPAY" -> KAKAOPAY_PG_CODE;
      case "TOSS", "TOSSPAY" -> TOSSPAY_PG_CODE;
      case "NAVER", "NAVERPAY" -> NAVERPAY_PG_CODE;
      default -> throw new IllegalArgumentException("지원하지 않는 PG사: " + pgProvider);
    };
  }

  // 결제 타입 변환
  private PaymentType getPaymentType(String pgCode) {
    if (pgCode.equals(KAKAOPAY_PG_CODE)) {
      return PaymentType.KAKAO;
    } else if (pgCode.equals(TOSSPAY_PG_CODE)) {
      return PaymentType.TOSS;
    } else if (pgCode.equals(NAVERPAY_PG_CODE)) {
      return PaymentType.NAVER;
    } else {
      return PaymentType.CARD;
    }
  }

  // 민감 정보 마스킹 처리
  private String maskString(String str) {
    if (str == null || str.length() <= 8) {
      return "****";
    }
    return str.substring(0, 4) + "*".repeat(str.length() - 8) + str.substring(str.length() - 4);
  }

  // 다음 결제일 계산
  private LocalDateTime calculateNextPaymentDate(LocalDateTime currentDate, ProductPeriod period) {
    return switch (period.name()) {
      case "MONTH" -> currentDate.plusMonths(1);
      case "YEAR" -> currentDate.plusYears(1);
      default -> currentDate.plusMonths(1); // 기본값은 1개월
    };
  }

  // 다음 결제일 및 구독 기간 저장
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

}