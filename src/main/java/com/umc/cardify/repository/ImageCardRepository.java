package com.umc.cardify.repository;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.StudyCardSet;

public interface ImageCardRepository extends JpaRepository<ImageCard, Long> {


	void deleteAllByStudyCardSet(StudyCardSet studyCardSet);

	List<ImageCard> findByStudyCardSet(StudyCardSet studyCardSet);

	@Query("SELECT ic FROM ImageCard ic WHERE ic.studyCardSet.user.userId = :userId AND ic.learnNextTime > :date")
	List<ImageCard> findAllByUserIdAndLearnNextTimeAfter(@Param("userId") Long userId, @Param("date") Timestamp date);
}