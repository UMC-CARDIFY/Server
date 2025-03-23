package com.umc.cardify.controller;

import com.umc.cardify.domain.enums.PaymentType;
import com.umc.cardify.dto.subscription.card.PaymentMethodRequest;
import com.umc.cardify.dto.subscription.card.PaymentMethodResponse;
import com.umc.cardify.domain.PaymentMethod;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.service.PaymentMethodService;
import com.umc.cardify.config.EncryptionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "PaymentMethodController", description = "결제 수단 관련 API")
@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final EncryptionUtil encryptionUtil;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "PAYMENT_METHOD_API_01 : 결제 수단 등록 API")
    public ResponseEntity<PaymentMethodResponse> registerPaymentMethod(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PaymentMethodRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 카드 번호는 마지막 4자리만 저장
        String maskedCardNumber = maskCardNumber(request.getCardNumber());

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(userRepository.findByName(userDetails.getUsername()))
                .type(PaymentType.CARD)
                .provider(request.getCardProvider())
                .cardNumber(maskedCardNumber)
                //.customerId(request.getCustomerUid()) // 빌링키
                .isDefault(request.isDefault())
                .build();

        // 새 카드를 기본 카드로 설정하는 경우, 기존 기본 카드 설정 해제
        if (request.isDefault()) {
            paymentMethodService.clearDefaultPaymentMethod(userDetails.getUsername());
        }

        PaymentMethod savedPaymentMethod = paymentMethodService.save(paymentMethod);
        return ResponseEntity.ok(new PaymentMethodResponse(savedPaymentMethod));
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<PaymentMethod> paymentMethods = paymentMethodService.findByUserId(userDetails.getUsername());
        List<PaymentMethodResponse> responses = paymentMethods.stream()
                .map(PaymentMethodResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        paymentMethodService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        PaymentMethod paymentMethod = paymentMethodService.setDefault(id, userDetails.getUsername());
        return ResponseEntity.ok(new PaymentMethodResponse(paymentMethod));
    }

}
