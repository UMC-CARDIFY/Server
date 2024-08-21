package com.umc.cardify.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.umc.cardify.domain.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.Card;

public interface CardRepository extends JpaRepository<Card, Long> {

	List<Card> findByStudyCardSet(StudyCardSet studyCardSet);

	@Query("SELECT c FROM Card c WHERE c.studyCardSet.user.userId = :userId AND c.learnNextTime > :date")
	List<Card> findAllByUserIdAndLearnNextTimeAfter(@Param("userId") Long userId, @Param("date") Timestamp date);

	@Query("SELECT c FROM Card c WHERE c.note.folder.user = :user AND c.learnLastTime BETWEEN :start AND :end")
	List<Card> findCardsByUserAndLearnLastTimeBetween(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
