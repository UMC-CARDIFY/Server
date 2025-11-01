package com.umc.cardify.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StudyLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "study_log_id")
	private Long studyLogId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_card_set_id")
	private StudyCardSet studyCardSet;

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

	@Column(name = "difficulty", nullable = false)
	private Integer difficulty; // 난이도 선택값 (1~4)

	@Column(name = "study_card_number", nullable = false)
	private int studyCardNumber;
}
