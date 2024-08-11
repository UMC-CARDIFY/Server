package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("SELECT f FROM Folder f WHERE f.user = :user ORDER BY " +
            "CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN f.markState = 'ACTIVE' THEN f.markDate END ASC, " +
            "CASE WHEN f.markState != 'ACTIVE' THEN f.createdAt END DESC")
    Page<Folder> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Folder f WHERE f.user = :user ORDER BY " +
            "CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN f.markState = 'ACTIVE' THEN f.markDate END ASC, " +
            "CASE WHEN :order = 'asc' THEN f.name " +
            "WHEN :order = 'edit-oldest' THEN f.editDate " +
            "END ASC, " +
            "CASE WHEN :order = 'desc' THEN f.name " +
            "WHEN :order = 'edit-newest' THEN f.editDate " +
            "END DESC")
    Page<Folder> findByUserAndSort(@Param("user") User user, @Param("order") String order, Pageable pageable);


    Optional<Folder> findByFolderIdAndUser(Long folderId, User userId);

    boolean existsByUserAndName(User user, String name);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.color IN (:color) ORDER BY " +
            "CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN f.markState = 'ACTIVE' THEN f.markDate END ASC, " +
            "CASE WHEN f.markState != 'ACTIVE' THEN f.createdAt END DESC")
    Page<Folder> findByUserAndColor(@Param("user") User user, @Param("color") String color, Pageable pageable);
}
