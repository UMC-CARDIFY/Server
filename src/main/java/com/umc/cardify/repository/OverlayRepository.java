package com.umc.cardify.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Overlay;

public interface OverlayRepository extends JpaRepository<Overlay, Long> {

	List<Overlay> findByImageCard(ImageCard imageCard);
}
