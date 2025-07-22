package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("SELECT f FROM Folder f WHERE f.user = :user ORDER BY " +
            "CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN f.markState = 'ACTIVE' THEN f.markDate END ASC, " +
            "CASE WHEN f.markState != 'ACTIVE' THEN f.createdAt END DESC")
    Page<Folder> findByUser(@Param("user") User user, Pageable pageable);

    List<Folder> findByUser(User user);

    Optional<Folder> findByFolderIdAndUser(Long folderId, User userId);

    boolean existsByUserAndName(User user, String name);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.color IN (:color) ORDER BY " +
            "CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN f.markState = 'ACTIVE' THEN f.markDate END ASC, " +
            "CASE WHEN f.markState != 'ACTIVE' THEN f.createdAt END DESC")
    List<Folder> findByUserAndColor(@Param("user") User user, @Param("color") List<String> color);

    // 특정 폴더의 하위 폴더 개수 count
    int countByParentFolder(Folder folder);

    // 사용자의 폴더 개수를 count
    int countByUserAndParentFolderIsNull(User user);

    List<Folder> findByUserAndParentFolderIsNull(User user);

    List<Folder> findByParentFolderAndUser(Folder parentFolder, User user);

    List<Folder> findAllByParentFolder(Folder folder);
}
