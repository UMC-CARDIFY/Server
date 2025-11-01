package com.umc.cardify.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.umc.cardify.domain.enums.CardType;
import com.umc.cardify.domain.enums.Difficulty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_card_set_id")
	private StudyCardSet studyCardSet;

    @Column(columnDefinition = "TEXT")
    private String contents;

	@Column(columnDefinition = "TEXT")
	private String contentsFront;

	@Column(columnDefinition = "TEXT")
	private String contentsBack;

	@Column(columnDefinition = "TEXT")
	private String answer;

	private int difficulty = 0;

	private Long countLearn;

	private Timestamp learnNextTime;

	private Timestamp learnLastTime;

	@Column(name = "type", nullable = false)
	private int type;  // 카드 타입

	@OneToMany(mappedBy = "card", cascade = CascadeType.ALL)
	private List<StudyLog> studyLogs = new ArrayList<>();

	@OneToMany(mappedBy = "card", cascade = CascadeType.ALL)
	private List<StudyHistory> studyHistories = new ArrayList<>();

	public void setCountLearn(Long countLearn) {
		this.countLearn = countLearn;
	}

	// Getter를 통해 enum 타입으로 가져오도록 설정
	public CardType getCardType() {
		return CardType.fromValue(this.type);
	}

	// Setter를 통해 enum을 int로 변환하여 설정
	public void setCardType(CardType cardType) {
		this.type = cardType.getValue();
	}

	public void setLearnNextTime(Timestamp learnNextTime) {
		this.learnNextTime = learnNextTime;
	}

	public void setLearnLastTime(Timestamp learnLastTime) {
		this.learnLastTime = learnLastTime;
	}

	public void setStudyCardSet(StudyCardSet studyCardSet) {
		this.studyCardSet = studyCardSet;
	}

	public Difficulty getDifficulty() {
		return Difficulty.fromValue(this.difficulty);
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

}
