package com.umc.cardify.domain;

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
public class Product extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @OneToMany(mappedBy = "product")
    private List<Subscription> subscriptions = new ArrayList<>();

    @Builder
    public Product(String name, Integer price, String period, Boolean isActive,
                   String description, LocalDateTime validFrom, LocalDateTime validUntil) {
        this.name = name;
        this.price = price;
        this.period = period;
        this.isActive = isActive;
        this.description = description;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }
}