package com.umc.cardify.controller;

import com.umc.cardify.dto.subscription.card.PaymentMethodRequest;
import com.umc.cardify.dto.subscription.card.PaymentMethodResponse;
import com.umc.cardify.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "PaymentMethodController", description = "결제 수단 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping
    @Operation(summary = "PAYMENT_METHOD_API_01 : 결제 수단 등록 API")
    public ResponseEntity<PaymentMethodResponse.registerPaymentMethodRes> registerPaymentMethod(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PaymentMethodRequest.registerPaymentReq request) {

        PaymentMethodResponse.registerPaymentMethodRes savedPaymentMethod = paymentMethodService.registerPaymentMethod(request, token);
        return ResponseEntity.ok(savedPaymentMethod);
    }

    // TODO : 이후 디자인에 보고 수정
    @GetMapping
    @Operation(summary = "PAYMENT_METHOD_API_02 : 결제 수단 목록 조회 API")
    public ResponseEntity<List<PaymentMethodResponse.registerPaymentMethodRes>> getPaymentMethods(
            @RequestHeader("Authorization") String token) {
        List<PaymentMethodResponse.registerPaymentMethodRes> responses = paymentMethodService.getPaymentMethods(token);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "PAYMENT_METHOD_API_03 : 결제 수단 삭제 API")
    public ResponseEntity<Long> deletePaymentMethod(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id) {

        paymentMethodService.deletePaymentMethod(id, token);
        return ResponseEntity.noContent().build();

    }

    @PutMapping("/{id}/default")
    @Operation(summary = "PAYMENT_METHOD_API_04 : 기본 결제 수단 변경 API")
    public ResponseEntity<PaymentMethodResponse.registerPaymentMethodRes> setDefaultPaymentMethod(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id) {

        PaymentMethodResponse.registerPaymentMethodRes paymentMethod = paymentMethodService.setDefaultPaymentMethod(id, token);
        return ResponseEntity.ok(paymentMethod);
    }

}
