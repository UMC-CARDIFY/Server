package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.CardType;
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

    @Column(columnDefinition = "TEXT")
    private String contentsFront;

    @Column(columnDefinition = "TEXT")
    private String contentsBack;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "Boolean DEFAULT false")
    private Boolean isLearn;

    private Difficulty difficulty;

    private Long countLearn;

    private Timestamp learnNextTime;

    private Timestamp learnLastTime;

    @Column(name = "type", nullable = false)
    private int type;  // 카드 타입

    // Getter를 통해 enum 타입으로 가져오도록 설정
    public CardType getCardType() {
        return CardType.fromValue(this.type);
    }

    // Setter를 통해 enum을 int로 변환하여 설정
    public void setCardType(CardType cardType) {
        this.type = cardType.getValue();
    }
}
