package com.umc.cardify.service;

import com.umc.cardify.config.exception.ResourceNotFoundException;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.domain.enums.PaymentType;
import com.umc.cardify.dto.subscription.card.PaymentMethodRequest;
import com.umc.cardify.repository.PaymentMethodRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    // 결제 수단 등록
    // TODO : userId 어떻게 할지
    @Transactional(readOnly = true)
    public PaymentMethod registerPaymentMethod(PaymentMethodRequest paymentMethodRequest, Long userId) {
        // 카드 번호는 마지막 4자리만 저장
        String maskedCardNumber = maskCardNumber(paymentMethodRequest.getCardNumber());

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(userRepository.findByName(userDetails.getUsername()))
                .type(PaymentType.CARD)
                .provider(paymentMethodRequest.getCardProvider())
                .cardNumber(maskedCardNumber)
                //.customerId(request.getCustomerUid()) // 빌링키
                .isDefault(paymentMethodRequest.isDefault())
                .build();

        // 새 카드를 기본 카드로 설정하는 경우, 기존 기본 카드 설정 해제
        if (paymentMethodRequest.isDefault()) {
            clearDefaultPaymentMethod(userDetails.getUsername());
        }

        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
    }



    // 결제 수단 목록 조회
    @Transactional(readOnly = true)
    public List<PaymentMethod> findByUserId(String userId) {
        return paymentMethodRepository.findByUserId(userId);
    }

    // 결제 수단 조회
    @Transactional(readOnly = true)
    public PaymentMethod findById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));
    }

    // 결제 수단 삭제
    @Transactional
    public void delete(Long id, String userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));

        // 기본 결제 수단이면서 다른 결제 수단이 있는 경우, 다른 결제 수단을 기본으로 설정
        if (paymentMethod.getIsDefault()) {
            List<PaymentMethod> otherPaymentMethods = paymentMethodRepository.findByUserIdAndIdNot(userId, id);
            if (!otherPaymentMethods.isEmpty()) {
                PaymentMethod newDefault = otherPaymentMethods.get(0);
                newDefault.setIsDefault(true);
                paymentMethodRepository.save(newDefault);
            }
        }

        paymentMethodRepository.delete(paymentMethod);
    }

    // 기본 결제 수단 변경
    @Transactional
    public PaymentMethod setDefault(Long id, String userId) {
        // 현재 기본 결제 수단 해제
        clearDefaultPaymentMethod(userId);

        // 새 기본 결제 수단 설정
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 수단을 찾을 수 없습니다"));

        paymentMethod.setIsDefault(true);
        return paymentMethodRepository.save(paymentMethod);
    }

    // 현재 기본 결제 수단 초기화
    @Transactional
    public void clearDefaultPaymentMethod(String userId) {
        List<PaymentMethod> defaultPaymentMethods = paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId);
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