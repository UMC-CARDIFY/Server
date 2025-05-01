package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor()
public class Subscription extends BaseEntity{

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Setter
    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Setter
    @Column
    private String cancelReason;

    @Setter
    @Column
    private LocalDateTime canceledAt;

    @Setter
    @Column(nullable = false)
    private Boolean autoRenew;

    @Setter
    @Column(nullable = false)
    private LocalDateTime nextPaymentDate;

    @OneToMany(mappedBy = "subscription")
    private List<SubscriptionPayment> payments = new ArrayList<>();

}