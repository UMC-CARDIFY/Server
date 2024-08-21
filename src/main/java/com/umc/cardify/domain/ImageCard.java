package com.umc.cardify.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.umc.cardify.domain.enums.Difficulty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ImageCard extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "study_card_set_id")
	private StudyCardSet studyCardSet;

	private int difficulty = 0;

	@Column(nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private Long width;

	@Column(nullable = false)
	private Long height;

	private Long countLearn;

	private Timestamp learnNextTime;

	private Timestamp learnLastTime;

	@OneToMany(mappedBy = "imageCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Overlay> overlays = new ArrayList<>();

	@Builder
	public ImageCard(String imageUrl, Long width, Long height, Long countLearn) {
		this.imageUrl = imageUrl;
		this.width = width;
		this.height = height;
		this.countLearn = countLearn;
	}

	// 연관관계 편의 메서드
	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
		overlay.setImageCard(this);
	}

	public Difficulty getDifficulty() {
		return Difficulty.fromValue(this.difficulty);
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}
}
