package com.umc.cardify.repository;

import com.umc.cardify.domain.SearchHistory;
import com.umc.cardify.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    SearchHistory findFirstByUserAndSearch(User user, String search);
    List<SearchHistory> findAllByUser(User user);
}
