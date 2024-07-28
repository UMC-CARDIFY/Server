package com.umc.cardify.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ImageCard extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String imageUrl;

	@Column(nullable = false)
	private Long width;

	@Column(nullable = false)
	private Long height;

	@OneToMany(mappedBy = "imageCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Overlay> overlays = new ArrayList<>();

	// 연관관계 편의 메서드
	public void addOverlay(Overlay overlay) {
		overlays.add(overlay);
		overlay.setImageCard(this);
	}

	@Builder

	public ImageCard(String imageUrl, Long width, Long height) {
		this.imageUrl = imageUrl;
		this.width = width;
		this.height = height;
	}
}