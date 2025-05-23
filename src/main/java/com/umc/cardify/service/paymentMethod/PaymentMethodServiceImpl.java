package com.umc.cardify.service.paymentMethod;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.domain.enums.PaymentType;
import com.umc.cardify.dto.payment.method.PaymentMethodRequest;
import com.umc.cardify.dto.payment.method.PaymentMethodResponse;
import com.umc.cardify.repository.PaymentMethodRepository;
import com.umc.cardify.repository.SubscriptionRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private Long findUserId(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함

        return userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();
    }

    // 1. 결제 수단 등록
    @Override
    @Transactional
    public PaymentMethodResponse.PaymentMethodInfoRes createPaymentMethod(PaymentMethodRequest.RegisterPaymentReq request, String token) {

        Long userId = findUserId(token);

        // 카드 번호는 마지막 4자리만 저장
        String maskedCardNumber = maskCardNumber(request.cardNumber());

        // 중복 검사
        if (paymentMethodRepository.existsByUserAndCardNumber(userRepository.findByUserId(userId), maskedCardNumber)) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_PAYMENT_METHOD);
        }

        // 유효 기간
        LocalDate validUntil = createExpiryDate(request.expiryYear(), request.expiryMonth());

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(userRepository.findByUserId(userId))
                .type(PaymentType.CARD)
                .provider(request.cardProvider())
                .cardNumber(maskedCardNumber)
                .isDefault(request.isDefault())
                .validUntil(validUntil)
                .build();

        // 새 카드를 기본 카드로 설정하는 경우, 기존 기본 카드 설정 해제
        if (request.isDefault()) {
            clearDefaultPaymentMethod(userId);
        }

        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        return new PaymentMethodResponse.PaymentMethodInfoRes(
            savedPaymentMethod.getId(),
            savedPaymentMethod.getType(),
            savedPaymentMethod.getProvider(),
            savedPaymentMethod.getCardNumber(),
            savedPaymentMethod.getIsDefault(),
            savedPaymentMethod.getCreatedAt()
        );

    }

    // TODO : 이후 디자인 보고 수정
    // 2. 결제 수단 목록 조회
    @Override
    public List<PaymentMethodResponse.PaymentMethodInfoRes> getPaymentMethods(String token) {
        Long userId = findUserId(token);

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUser_UserId(userId);
        List<PaymentMethodResponse.PaymentMethodInfoRes> responses = paymentMethods.stream()
                .map(PaymentMethodResponse.PaymentMethodInfoRes::new)
                .collect(Collectors.toList());

        return responses;
    }


    // 3. 결제 수단 삭제
    @Override
    @Transactional
    public void deletePaymentMethod(Long id, String token) {
        Long userId = findUserId(token);

        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다: " + id));

        // 현재 사용 중인 구독이 있는지 확인
        boolean hasActiveSubscription = subscriptionRepository.existsByPaymentMethodAndStatusIn(
            id,
            Arrays.asList("ACTIVE", "PENDING")
        );

        if (hasActiveSubscription) {
            throw new IllegalStateException("이 결제 수단을 사용 중인 활성 구독이 있습니다");
        }

        // 논리적 삭제 수행
        paymentMethod.setDeletedAt(LocalDateTime.now());
        paymentMethodRepository.save(paymentMethod);

        // 기본 결제 수단이면서 다른 결제 수단이 있는 경우, 뒤의 결제 수단을 기본으로 설정
        if (paymentMethod.getIsDefault()) {
            List<PaymentMethod> otherPaymentMethods = paymentMethodRepository.findByUser_UserIdAndIdNot(userId, id);
            if (!otherPaymentMethods.isEmpty()) {
                PaymentMethod newDefault = otherPaymentMethods.get(0);
                newDefault.setIsDefault(true);
                paymentMethodRepository.save(newDefault);
            }
        }
    }

    // TODO : 이후 디자인에 보고 수정
    // 4. 기본 결제 수단 변경
    @Override
    @Transactional
    public PaymentMethodResponse.PaymentMethodInfoRes setDefaultPaymentMethod(Long id, String token) {
        Long userId = findUserId(token);
        // 현재 기본 결제 수단 해제
        clearDefaultPaymentMethod(userId);
        // 새 기본 결제 수단 설정
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUser_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));

        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);

        return new PaymentMethodResponse.PaymentMethodInfoRes(
            id,
            paymentMethod.getType(),
            paymentMethod.getProvider(),
            paymentMethod.getCardNumber(),
            paymentMethod.getIsDefault(),
            paymentMethod.getCreatedAt()
        );
    }

    // 현재 기본 결제 수단 초기화
    @Override
    @Transactional
    public void clearDefaultPaymentMethod(Long userId) {
        List<PaymentMethod> defaultPaymentMethods = paymentMethodRepository.findByUser_UserIdAndIsDefaultTrue(userId);
        for (PaymentMethod pm : defaultPaymentMethods) {
            pm.setIsDefault(false);
            paymentMethodRepository.save(pm);
        }
    }

    // 카드번호 마스킹 (앞 6자리와 뒤 4자리만 유지)
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) {
            return "Invalid card number";
        }

        // 하이픈(-) 제거
        String normalized = cardNumber.replaceAll("-", "");

        // 앞 6자리와 뒤 4자리만 유지하고 나머지는 마스킹
        int length = normalized.length();
        return normalized.substring(0, 6) +
                "*".repeat(length - 10) +
                normalized.substring(length - 4);
    }

    // 유효 기간 설정
    private LocalDate createExpiryDate(int expiryYear, int expiryMonth) {
        // 현재 세기(2000년대)를 기준으로 2000 + 입력된 연도(YY)로 변환
        int fullYear = 2000 + expiryYear;

        // 해당 월의 마지막 날짜 구하기
        YearMonth yearMonth = YearMonth.of(fullYear, expiryMonth);
        return yearMonth.atEndOfMonth();
    }
}