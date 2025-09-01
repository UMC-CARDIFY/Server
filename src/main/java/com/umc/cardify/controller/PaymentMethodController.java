package com.umc.cardify.controller;

import com.umc.cardify.dto.payment.method.PaymentMethodRequest;
import com.umc.cardify.dto.payment.method.PaymentMethodResponse;
import com.umc.cardify.service.paymentMethod.PaymentMethodServiceImpl;
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

    private final PaymentMethodServiceImpl paymentMethodServiceImpl;

    @PostMapping
    @Operation(summary = "PAYMENT_METHOD_API_01 : 결제 수단 등록 API")
    public ResponseEntity<PaymentMethodResponse.PaymentMethodInfoRes> registerPaymentMethod(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody PaymentMethodRequest.RegisterPaymentReq request) {

        PaymentMethodResponse.PaymentMethodInfoRes savedPaymentMethod = paymentMethodServiceImpl.createPaymentMethod(request, token);
        return ResponseEntity.ok(savedPaymentMethod);
    }

    // TODO : 이후 디자인에 보고 수정
    @GetMapping
    @Operation(summary = "PAYMENT_METHOD_API_02 : 결제 수단 목록 조회 API")
    public ResponseEntity<List<PaymentMethodResponse.PaymentMethodInfoRes>> getPaymentMethods(
            @RequestHeader("Authorization") String token) {
        List<PaymentMethodResponse.PaymentMethodInfoRes> responses = paymentMethodServiceImpl.getPaymentMethods(token);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "PAYMENT_METHOD_API_03 : 결제 수단 삭제 API")
    public ResponseEntity<Long> deletePaymentMethod(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id) {

        paymentMethodServiceImpl.deletePaymentMethod(id, token);
        return ResponseEntity.ok(id);

    }

    @PutMapping("/{id}/default")
    @Operation(summary = "PAYMENT_METHOD_API_04 : 기본 결제 수단 변경 API")
    public ResponseEntity<PaymentMethodResponse.PaymentMethodInfoRes> setDefaultPaymentMethod(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id) {

        PaymentMethodResponse.PaymentMethodInfoRes paymentMethod = paymentMethodServiceImpl.setDefaultPaymentMethod(id, token);
        return ResponseEntity.ok(paymentMethod);
    }

}
