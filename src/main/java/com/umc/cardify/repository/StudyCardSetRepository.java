package com.umc.cardify.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.StudyCardSet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyCardSetRepository extends JpaRepository<StudyCardSet, Long> {

	Optional<StudyCardSet> findByNote(Note note);

	List<StudyCardSet> findByUserUserId(Long userId);

	boolean existsByNote(Note note);

	@Query("SELECT s FROM StudyCardSet s WHERE s.user.userId = :userId ORDER BY s.nextStudyDate ASC")
	List<StudyCardSet> findByUserOrderByNextStudyDateAsc(@Param("userId") Long userId);
}
