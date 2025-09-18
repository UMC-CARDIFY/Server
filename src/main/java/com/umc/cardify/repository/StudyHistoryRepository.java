package com.umc.cardify.repository;

import com.umc.cardify.domain.StudyHistory;
import com.umc.cardify.domain.StudyLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyHistoryRepository extends JpaRepository<StudyHistory, Long> {
    /**
     * 특정 유저의 연간 학습 로그를 일자별로 집계
     *  - 하루 학습 카드 수 (SUM)
     *  - 같은 주 내에서 가장 큰 학습 수 (MAX)
     */
    @Query(value =
            "SELECT t.log_date, " +
                    "       t.daily_count, " +
                    "       MAX(t.daily_count) OVER (PARTITION BY YEARWEEK(t.log_date, 1)) AS week_max " +
                    "FROM ( " +
                    "    SELECT DATE(s.study_date) AS log_date, " +
                    "           SUM(s.studied_card_count) AS daily_count " +
                    "    FROM study_history s " +
                    "    WHERE s.user_id = :userId " +
                    "      AND s.study_date BETWEEN :startDate AND :endDate " +
                    "    GROUP BY DATE(s.study_date) " +
                    ") t " +
                    "ORDER BY t.log_date ASC",
            nativeQuery = true)
    List<Object[]> findDailyCountWithWeekMaxNative(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
