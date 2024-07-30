package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query(value = "SELECT * FROM folder WHERE user_id = :userId ORDER BY " +
            "CASE WHEN mark_state = 'ACTIVE' THEN 0 " +
            "WHEN mark_state = 'INACTIVE' THEN 1 " +
            "ELSE 2 END, " +
            "mark_date ASC, created_at ASC", nativeQuery = true)
    Page<Folder> findByUser(@Param("userId") User userId, Pageable pageable);

    Optional<Folder> findByFolderIdAndUser(Long folderId, User userId);

    boolean existsByUserAndName(User user, String name);

    @Query(value = "SELECT * FROM folder WHERE user_id = :userId AND color IN (:colors) ORDER BY " +
            "CASE WHEN mark_state = 'ACTIVE' THEN 0 " +
            "WHEN mark_state = 'INACTIVE' THEN 1 " +
            "ELSE 2 END, " +
            "mark_date ASC, created_at ASC", nativeQuery = true)
    Page<Folder> findByUserAndColor(@Param("userId") User userId, @Param("colors") String colors, Pageable pageable);

}
