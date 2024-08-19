package com.umc.cardify.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
	List<Card> findByNote(Note note);

	List<Card> findByStudyCardSet(StudyCardSet studyCardSet);
}
