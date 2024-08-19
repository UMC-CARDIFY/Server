package com.umc.cardify.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.cardify.domain.StudyLog;

public interface StudyLogRepository extends JpaRepository<StudyLog, Long> {
}
