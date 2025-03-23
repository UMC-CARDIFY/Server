package com.umc.cardify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

// 민감한 데이터를 암호화하고 복호화하는 유틸리티 클래스
@Component
@Slf4j
public class EncryptionUtil {

    private final SecretKey secretKey;
    private final byte[] iv;

    // 환경 변수에서 암호화 키를 받아 초기화합니다.
    public EncryptionUtil(@Value("${encryption.key}") String encryptionKey) {
        try {
            // 키 생성 (AES-256 사용을 위해 32바이트로 확장)
            byte[] keyBytes = Arrays.copyOf(encryptionKey.getBytes(StandardCharsets.UTF_8), 32);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");

            // 초기화 벡터 생성 (16바이트 필요)
            this.iv = new byte[16];
            new SecureRandom().nextBytes(iv);
        } catch (Exception e) {
            log.error("암호화 유틸리티 초기화 오류: {}", e.getMessage(), e);
            throw new SecurityException("암호화 유틸리티 초기화 실패");
        }
    }

    // 평문 텍스트를 암호화합니다.
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호화된 데이터를 합쳐서 Base64 인코딩
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("암호화 중 오류 발생: {}", e.getMessage(), e);
            throw new SecurityException("데이터 암호화 처리 중 오류가 발생했습니다");
        }
    }

    // 암호화된 텍스트를 복호화합니다.
    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // IV 추출
            byte[] extractedIv = new byte[16];
            System.arraycopy(combined, 0, extractedIv, 0, extractedIv.length);

            // 암호화된 데이터 추출
            byte[] encryptedData = new byte[combined.length - 16];
            System.arraycopy(combined, 16, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(extractedIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            byte[] original = cipher.doFinal(encryptedData);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("복호화 중 오류 발생: {}", e.getMessage(), e);
            throw new SecurityException("데이터 복호화 처리 중 오류가 발생했습니다");
        }
    }

    // 카드 번호를 마스킹 처리합니다.
    // 앞 6자리와 뒤 4자리는 그대로 유지하고 나머지는 '*'로 대체합니다.
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) {
            return "Invalid card number";
        }

        // 하이픈(-) 제거
        String normalized = cardNumber.replaceAll("[-\\s]", "");

        // 앞 6자리와 뒤 4자리만 유지하고 나머지는 마스킹
        int length = normalized.length();
        return normalized.substring(0, 6) +
                "*".repeat(length - 10) +
                normalized.substring(length - 4);
    }
}