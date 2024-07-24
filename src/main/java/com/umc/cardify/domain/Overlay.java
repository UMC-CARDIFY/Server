package com.umc.cardify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
	import jakarta.persistence.FetchType;
	import jakarta.persistence.GeneratedValue;
	import jakarta.persistence.GenerationType;
	import jakarta.persistence.Id;
	import jakarta.persistence.JoinColumn;
	import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class Overlay extends BaseEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long xPosition;
	@Column(nullable = false)
	private Long yPosition;
	@Column(nullable = false)
	private Long width;
	@Column(nullable = false)
	private Long height;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_card_id")
	private ImageCard imageCard;

	public void setImageCard(ImageCard imageCard) {
		this.imageCard = imageCard;
	}

	@Builder
	public Overlay(Long xPosition, Long yPosition, Long width, Long height, ImageCard imageCard) {
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.width = width;
		this.height = height;
		this.imageCard = imageCard;
	}
}
