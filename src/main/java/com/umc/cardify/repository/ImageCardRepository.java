package com.umc.cardify.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.StudyCardSet;

public interface ImageCardRepository extends JpaRepository<ImageCard, Long> {

	void deleteAllByStudyCardSet(StudyCardSet studyCardSet);

	List<ImageCard> findByStudyCardSet(StudyCardSet studyCardSet);

	@Query("SELECT ic FROM ImageCard ic WHERE ic.studyCardSet.user.userId = :userId AND DATE(ic.learnNextTime) = DATE(:date)")
	List<ImageCard> findAllByUserIdAndLearnNextTimeOnDate(@Param("userId") Long userId, @Param("date") Timestamp date);

	@Query("SELECT ic FROM ImageCard ic WHERE ic.studyCardSet.user.userId = :userId AND ic.learnNextTime BETWEEN :start AND :end")
	List<ImageCard> findAllByUserIdAndLearnNextTimeBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	@Query("SELECT i.difficulty FROM ImageCard i WHERE i.id = :cardId")
	int findImageCardDifficultyByCardId(@Param("cardId") Long cardId);
}