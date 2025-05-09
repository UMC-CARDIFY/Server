package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.ProductPeriod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity{

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;


    // 연간 상품, 월간 상품
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductPeriod period;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(columnDefinition = "TEXT")
    private String description;

//    @Column(nullable = false)
//    private LocalDateTime validUntil;

    @OneToMany(mappedBy = "product")
    private List<Subscription> subscriptions = new ArrayList<>();
}