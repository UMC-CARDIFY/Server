package com.umc.cardify.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.User;

public interface StudyCardSetRepository extends JpaRepository<StudyCardSet, Long> {

	Optional<StudyCardSet> findByNote(Note note);

	Page<StudyCardSet> findByUser(User user, Pageable pageable);
}
