package com.umc.cardify.service.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.Product;
import com.umc.cardify.domain.Subscription;
import com.umc.cardify.domain.SubscriptionPayment;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.domain.enums.SubscriptionStatus;
import com.umc.cardify.dto.payment.subscription.SubscriptionRequest;
import com.umc.cardify.dto.payment.subscription.SubscriptionResponse;
import com.umc.cardify.repository.*;
import com.umc.cardify.service.payment.KakaoPaymentServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final KakaoPaymentServiceImpl kakaoPaymentServiceImpl;

  private Long findUserId(String token) {
    String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
    AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함

    return userRepository.findByEmailAndProvider(email, provider)
        .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();
  }

  @Override
  @Transactional
  public SubscriptionResponse.SubscriptionInfoRes createSubscription(SubscriptionRequest.CreateSubscriptionReq request, String token) {
    if (token != null) {
      Long userId = findUserId(token);
    }

    log.info("구독 생성 시작: userId={}, productId={}", request.userId(), request.productId());

    // 상품 정보 조회
    Product product = productRepository.findById(request.productId())
        .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다: " + request.productId()));

    // 결제 수단 조회
    PaymentMethod paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
        .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다: " + request.paymentMethodId()));

    // 사용자 ID 일치 여부 확인
    if (!paymentMethod.getUser().getUserId().equals(request.userId())) {
      throw new IllegalArgumentException("해당 사용자의 결제 수단이 아닙니다");
    }

    // 구독 생성
    LocalDateTime nextPaymentDateTime = kakaoPaymentServiceImpl.
        calculateNextPaymentDate(LocalDateTime.now(), product.getPeriod());

    // 월말 처리 (예: 1월 31일 -> 2월 28일)
    int currentDay = LocalDateTime.now().getDayOfMonth();
    Month nextMonth = nextPaymentDateTime.getMonth();
    int daysInNextMonth = nextMonth.length(Year.isLeap(nextPaymentDateTime.getYear()));

    if (currentDay > daysInNextMonth) {
      // 다음 달의 마지막 날로 조정
      nextPaymentDateTime = nextPaymentDateTime.withDayOfMonth(daysInNextMonth);
    }
    Subscription subscription = Subscription.builder()
        .user(userRepository.findByUserId(request.userId()))
        .product(product)
        .status(SubscriptionStatus.ACTIVE)
        .startDate(LocalDateTime.now())
        .nextPaymentDate(nextPaymentDateTime)
        .autoRenew(true)
        .endDate(LocalDateTime.now().plusMonths(1))
        .build();

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("구독 생성 완료: id={}", savedSubscription.getId());

    return getSubscriptionInternal(savedSubscription.getId());
  }

  // 구독 조회
  @Override
  public SubscriptionResponse.SubscriptionInfoRes getSubscription(Long subscriptionId, String token) {
    Long userId = findUserId(token);
    return getSubscriptionInternal(subscriptionId);
  }

  // 내부용 메소드 (토큰 없이 호출 가능)
  private SubscriptionResponse.SubscriptionInfoRes getSubscriptionInternal(Long subscriptionId) {
    Subscription subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new ResourceNotFoundException("구독을 찾을 수 없습니다: " + subscriptionId));

    Product product = productRepository.findById(subscription.getProduct().getId())
        .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다: " + subscription.getProduct().getId()));

    // 결제 수단 조회
    PaymentMethod paymentMethod = paymentMethodRepository.findByUser_UserIdAndIsDefaultTrueAndDeletedAtIsNull(subscription.getUser().getUserId())
        .orElse(null);

    SubscriptionResponse.SubscriptionInfoRes.PaymentMethodInfo paymentMethodInfo = null;
    if (paymentMethod != null) {
      paymentMethodInfo = SubscriptionResponse.SubscriptionInfoRes.PaymentMethodInfo.builder()
          .id(paymentMethod.getId())
          .type(paymentMethod.getType().name())
          .provider(paymentMethod.getProvider())
          .cardNumber(paymentMethod.getCardNumber())
          .build();
    }

    return SubscriptionResponse.SubscriptionInfoRes.builder()
        .id(subscription.getId())
        .userId(subscription.getUser().getUserId())
        .productId(product.getId())
        .productName(product.getName())
        .status(SubscriptionStatus.ACTIVE.name())
        .startDate(subscription.getStartDate())
        .nextPaymentDate(subscription.getNextPaymentDate() != null ?
            subscription.getNextPaymentDate() : null)
        .autoRenew(subscription.getAutoRenew())
        .paymentMethod(paymentMethodInfo)
        .build();
  }

  @Override
  public SubscriptionResponse.SubscriptionListRes getSubscriptionsByUserId(String token) {
    Long userId = findUserId(token);
    List<Subscription> subscriptions = subscriptionRepository.findByUser_UserId(userId);

    List<SubscriptionResponse.SubscriptionInfoRes> subscriptionDTOs = subscriptions.stream()
        .map(subscription -> getSubscriptionInternal(subscription.getId()))
        .collect(Collectors.toList());

    return SubscriptionResponse.SubscriptionListRes.builder()
        .subscriptions(subscriptionDTOs)
        .totalCount(subscriptionDTOs.size())
        .build();
  }

  // 구독 최소
  @Override
  @Transactional
  public boolean cancelSubscription(SubscriptionRequest.CancelSubscriptionReq request, String token) {
    Long userId = findUserId(token);
    log.info("구독 취소 요청: id={}, reason={}", request.subscriptionId(), request.cancelReason());

    Subscription subscription = subscriptionRepository.findById(request.subscriptionId())
        .orElseThrow(() -> new ResourceNotFoundException("구독을 찾을 수 없습니다: " + request.subscriptionId()));

    // 이미 취소된 구독인지 확인
    if ("CANCELLED".equals(subscription.getStatus().name())) {
      log.warn("이미 취소된 구독입니다: id={}", request.subscriptionId());
      return false;
    }

    // 구독 상태 변경
    subscription.setStatus(SubscriptionStatus.CANCELLED);
    subscription.setCancelReason(request.cancelReason());
    subscription.setCanceledAt(LocalDateTime.now());
    subscription.setAutoRenew(false);

    subscriptionRepository.save(subscription);
    log.info("구독 취소 완료: id={}", request.subscriptionId());

    return true;
  }

  // 자동 결제(갱신) 변경
  @Override
  @Transactional
  public boolean updateAutoRenew(Long subscriptionId, boolean autoRenew, String token) {
    Long userId = findUserId(token);
    Subscription subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new ResourceNotFoundException("구독을 찾을 수 없습니다: " + subscriptionId));

    subscription.setAutoRenew(autoRenew);
    subscriptionRepository.save(subscription);

    log.info("자동 갱신 설정 변경: subscriptionId={}, autoRenew={}", subscriptionId, autoRenew);

    return true;
  }

  // 특정 구독의 결제 이력 조회
  @Override
  public SubscriptionResponse.PaymentHistoryListRes getPaymentHistoriesBySubscriptionId(Long subscriptionId, String token) {
    Long userId = findUserId(token);
    List<SubscriptionPayment> payments = subscriptionPaymentRepository.findBySubscriptionIdOrderByPaidAtDesc(subscriptionId);

    List<SubscriptionResponse.PaymentHistoryRes> paymentDTOs = payments.stream()
        .map(this::convertToPaymentHistoryRes)
        .collect(Collectors.toList());

    return SubscriptionResponse.PaymentHistoryListRes.builder()
        .payments(paymentDTOs)
        .totalCount(paymentDTOs.size())
        .build();
  }

  private SubscriptionResponse.PaymentHistoryRes convertToPaymentHistoryRes(SubscriptionPayment entity) {
    LocalDateTime paidAt = null;
    if (entity.getPaidAt() != null) {
      paidAt = entity.getPaidAt();
    }

    return SubscriptionResponse.PaymentHistoryRes.builder()
        .id(entity.getId())
        .subscriptionId(entity.getSubscription().getId())
        .paymentMethodId(entity.getPaymentMethod().getId())
        .merchantUid(entity.getMerchantUid())
        .status(entity.getStatus().name())
        .amount(entity.getAmount())
        .paidAt(paidAt)
        .pgProvider(entity.getPgProvider())
        .build();
  }

}
