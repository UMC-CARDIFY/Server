package com.umc.cardify.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.StudyCardSet;

public interface ImageCardRepository extends JpaRepository<ImageCard, Long> {

	List<ImageCard> findByStudyCardSet(StudyCardSet studyCardSet);
}