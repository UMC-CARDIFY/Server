package com.umc.cardify.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.ImageCard;

public interface ImageCardRepository extends JpaRepository<ImageCard, Long> {
}