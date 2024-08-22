package com.umc.cardify.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.User;

public interface CardRepository extends JpaRepository<Card, Long> {
	@Modifying
	@Query("DELETE FROM Card c WHERE c.note.noteId = :noteId")
	void deleteCardsByNoteId(@Param("noteId") Long noteId);

	List<Card> findByStudyCardSet(StudyCardSet studyCardSet);

	@Query("SELECT c FROM Card c WHERE c.studyCardSet.user.userId = :userId AND DATE(c.learnNextTime) = DATE(:date)")
	List<Card> findAllByUserIdAndLearnNextTimeOnDate(@Param("userId") Long userId, @Param("date") Timestamp date);

	@Query("SELECT c FROM Card c WHERE c.note.folder.user = :user AND c.learnLastTime BETWEEN :start AND :end")
	List<Card> findCardsByUserAndLearnLastTimeBetween(@Param("user") User user, @Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end);

	@Query("SELECT c FROM Card c WHERE c.studyCardSet.user.userId = :userId AND c.learnNextTime BETWEEN :start AND :end")
	List<Card> findAllByUserIdAndLearnNextTimeBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

