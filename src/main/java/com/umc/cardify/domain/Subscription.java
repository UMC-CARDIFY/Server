package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.SubscriptionStatus;
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
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private String cancelReason;

    private LocalDateTime canceledAt;

    @Column(nullable = false)
    private Boolean autoRenew;

    @Column(nullable = false)
    private LocalDateTime nextPaymentDate;

    @OneToMany(mappedBy = "subscription")
    private List<SubscriptionPayment> payments = new ArrayList<>();

    @Builder
    public Subscription(Product product, User user, SubscriptionStatus status,
                        LocalDateTime startDate, LocalDateTime endDate, Boolean autoRenew,
                        LocalDateTime nextPaymentDate) {
        this.product = product;
        this.user = user;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.autoRenew = autoRenew;
        this.nextPaymentDate = nextPaymentDate;
    }
}