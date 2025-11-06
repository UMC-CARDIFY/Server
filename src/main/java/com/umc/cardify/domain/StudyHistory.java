package com.umc.cardify.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StudyHistory extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_history_id")
    private Long studyHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = true)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_card_id", nullable = true)
    private ImageCard imageCard;

    @Column(name = "study_date", nullable = false)
    private LocalDateTime studyDate;

    @Column(name = "total_learn_count", nullable = false)
    private Integer totalLearnCount;

//    @Column(name = "last_learned_at", nullable = false)
//    private LocalDateTime lastLearnedAt;

    public void setTotalLearnCount(Integer totalLearnCount) {
        this.totalLearnCount = totalLearnCount;
    }

//    public void setLastLearnedAt(LocalDateTime lastLearnedAt) {
//        this.lastLearnedAt = lastLearnedAt;
//    }
}
