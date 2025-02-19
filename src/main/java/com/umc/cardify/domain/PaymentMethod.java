package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(nullable = false)
    private String provider;

    private String cardNumber;

    private LocalDateTime validUntil;

    @Column(nullable = false)
    private Boolean isDefault;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "paymentMethod")
    private List<SubscriptionPayment> payments = new ArrayList<>();

    @Builder
    public PaymentMethod(User user, PaymentType type, String provider,
                         String cardNumber, LocalDateTime validUntil, Boolean isDefault) {
        this.user = user;
        this.type = type;
        this.provider = provider;
        this.cardNumber = cardNumber;
        this.validUntil = validUntil;
        this.isDefault = isDefault;
    }
}
