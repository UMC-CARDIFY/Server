package com.umc.cardify.service;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.enums.PaymentType;
import com.umc.cardify.dto.subscription.card.PaymentMethodRequest;
import com.umc.cardify.dto.subscription.card.PaymentMethodResponse;
import com.umc.cardify.repository.PaymentMethodRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private Long findUserId(String token) {
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID))
                .getUserId();
        return userId;
    }

    // 1. 결제 수단 등록
    @Transactional
    public PaymentMethodResponse.registerPaymentMethodRes registerPaymentMethod(PaymentMethodRequest.registerPaymentReq paymentMethodRequest, String token) {

        Long userId = findUserId(token);

        // 카드 번호는 마지막 4자리만 저장
        String maskedCardNumber = maskCardNumber(paymentMethodRequest.cardNumber());

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(userRepository.findByUserId(userId))
                .type(PaymentType.CARD)
                .provider(paymentMethodRequest.cardProvider())
                .cardNumber(maskedCardNumber)
                .isDefault(paymentMethodRequest.isDefault())
                .build();

        // 새 카드를 기본 카드로 설정하는 경우, 기존 기본 카드 설정 해제
        if (paymentMethodRequest.isDefault()) {
            clearDefaultPaymentMethod(userId);
        }

        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        return PaymentMethodResponse.registerPaymentMethodRes.builder()
                .id(savedPaymentMethod.getId())
                .type(savedPaymentMethod.getType())
                .provider(savedPaymentMethod.getProvider())
                .cardNumber(savedPaymentMethod.getCardNumber())
                .isDefault(savedPaymentMethod.getIsDefault())
                .createdAt(savedPaymentMethod.getCreatedAt())
                .build();

    }

    // TODO : 이후 디자인에 보고 수정
    // 2. 결제 수단 목록 조회
    public List<PaymentMethodResponse.registerPaymentMethodRes> getPaymentMethods(String token) {
        Long userId = findUserId(token);

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUser_UserId(userId);
        List<PaymentMethodResponse.registerPaymentMethodRes> responses = paymentMethods.stream()
                .map(PaymentMethodResponse.registerPaymentMethodRes::new)
                .collect(Collectors.toList());

        return responses;
    }

    // 결제 수단 조회
    @Transactional(readOnly = true)
    public PaymentMethod findById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));
    }

    // 3. 결제 수단 삭제
    @Transactional
    public void deletePaymentMethod(Long id, String token) {
        Long userId = findUserId(token);
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUser_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));

        // 기본 결제 수단이면서 다른 결제 수단이 있는 경우, 다른 결제 수단을 기본으로 설정
        if (paymentMethod.getIsDefault()) {
            List<PaymentMethod> otherPaymentMethods = paymentMethodRepository.findByUser_UserIdAndIdNot(userId, id);
            if (!otherPaymentMethods.isEmpty()) {
                PaymentMethod newDefault = otherPaymentMethods.get(0);
                newDefault.setIsDefault(true);
                paymentMethodRepository.save(newDefault);
            }
        }
        paymentMethodRepository.delete(paymentMethod);
    }

    // TODO : 이후 디자인에 보고 수정
    // 4. 기본 결제 수단 변경
    @Transactional
    public PaymentMethodResponse.registerPaymentMethodRes setDefaultPaymentMethod(Long id, String token) {
        Long userId = findUserId(token);
        // 현재 기본 결제 수단 해제
        clearDefaultPaymentMethod(userId);
        // 새 기본 결제 수단 설정
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUser_UserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));

        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);

        return PaymentMethodResponse.registerPaymentMethodRes.builder().build();
    }

    // 현재 기본 결제 수단 초기화
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
}