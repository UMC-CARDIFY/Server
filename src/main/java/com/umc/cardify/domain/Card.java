package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.sql.Timestamp;

@Entity
@Getter
@DynamicUpdate
@DynamicInsert
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Card extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String contentsFront;

    @Column(columnDefinition = "TEXT")
    private String contentsBack;

    @Column(columnDefinition = "Boolean DEFAULT false")
    private Boolean isLearn;

    private Difficulty difficulty;

    private Long countLearn;

    private Timestamp learnNextTime;

    private Timestamp learnLastTime;
}
