package com.umc.cardify.repository;

import com.umc.cardify.domain.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudyHistoryRepository extends JpaRepository<StudyHistory, Long> {
    /**
     * 서브 쿼리 방식으로 GROUP BY -> 윈도우 함수 순으로 처리
     * @name 특정 유저의 연간 학습 로그를 일자별로 집계 | 하루 학습 카드 수 (SUM) | 같은 주 내에서 가장 큰 학습 수 (MAX)
     */
    @Query(value = """
        SELECT 
            t.study_date,
            t.daily_count,
            MAX(t.daily_count) OVER (PARTITION BY YEARWEEK(t.study_date)) AS week_max
        FROM (
            SELECT 
                DATE(sh.study_date) AS study_date,
                SUM(sh.total_learn_count) AS daily_count
            FROM study_history sh
            WHERE sh.user_id = :userId
            AND sh.study_date BETWEEN :start AND :end
            GROUP BY DATE(sh.study_date)
        ) AS t
        ORDER BY t.study_date ASC
    """, nativeQuery = true)
    List<Object[]> findDailyCountWithWeekMaxNative(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<StudyHistory> findByUserAndCard(User user, Card card);
    Optional<StudyHistory> findByUserAndImageCard(User user, ImageCard imageCard);
}
