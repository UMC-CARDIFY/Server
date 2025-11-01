package com.umc.cardify.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.StudyLog;

public interface StudyLogRepository extends JpaRepository<StudyLog, Long> {

	Page<StudyLog> findByStudyCardSet(StudyCardSet studyCardSet, Pageable pageable);

	void deleteByUser_UserIdAndCard_CardId(Long userId, Long cardId);

	void deleteByUser_UserIdAndImageCard_Id(Long userId, Long cardId);
}
