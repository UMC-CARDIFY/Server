package com.umc.cardify.repository;

import com.umc.cardify.domain.Download;
import com.umc.cardify.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DownloadRepository extends JpaRepository<Download, Long> {
    List<Download> findByUser(User user);
}