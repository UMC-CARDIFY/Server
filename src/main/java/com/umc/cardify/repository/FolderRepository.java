package com.umc.cardify.repository;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUser(User user);

    Optional<Folder> findByFolderIdAndUser(Long folderId, User userId);

    boolean existsByUserAndName(User user, String name);

    // 특정 폴더의 하위 폴더 개수 count
    int countByParentFolder(Folder folder);

    // 사용자의 폴더 개수를 count
    int countByUserAndParentFolderIsNull(User user);

    List<Folder> findByUserAndParentFolderIsNull(User user);

    List<Folder> findByParentFolderAndUser(Folder parentFolder, User user);

    List<Folder> findAllByParentFolder(Folder folder);

    // 하위폴더 이동 - 상위폴더 검색 (키워드 포함)
    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.parentFolder IS NULL " +
            "AND f.name LIKE %:keyword% ORDER BY CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, f.name ASC")
    Page<Folder> findParentFoldersByKeyword(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    // 하위폴더 이동 - 전체 상위폴더 조회
    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.parentFolder IS NULL " +
            "ORDER BY CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, f.name ASC")
    Page<Folder> findAllParentFolders(@Param("user") User user, Pageable pageable);

    // 하위폴더 이동 - 하위폴더 개수 조회
    @Query("SELECT COUNT(f) FROM Folder f WHERE f.parentFolder = :parentFolder")
    Long countSubFolders(@Param("parentFolder") Folder parentFolder);

    // 하위폴더 이동 - 이동 가능한 상위폴더 개수 (현재 상위폴더 제외)
    @Query("SELECT COUNT(f) FROM Folder f WHERE f.user = :user AND f.parentFolder IS NULL " +
            "AND f.folderId != :currentParentFolderId")
    Long countAvailableParentFolders(@Param("user") User user, @Param("currentParentFolderId") Long currentParentFolderId);

    // 노트 이동 - 폴더 검색 (키워드 포함)
    @Query("SELECT f FROM Folder f " +
            "LEFT JOIN f.parentFolder pf " +
            "WHERE f.user = :user " +
                "AND ( " +
            "            f.name LIKE %:keyword%" +
            "         OR pf.name LIKE %:keyword% )" +
            "ORDER BY CASE WHEN f.markState = 'ACTIVE' THEN 0 ELSE 1 END, f.name ASC")
    Page<Folder> findFoldersByKeyword(User user, @Param("keyword") String keyword, Pageable pageable);

    // 특정 상위폴더들을 하위폴더와 함께 조회
    @Query("SELECT DISTINCT p FROM Folder p " +
            "LEFT JOIN FETCH p.subFolders " +
            "WHERE p.user = :user " +
            "AND p.parentFolder IS NULL " +
            "AND p.folderId IN :parentFolderIds")
    List<Folder> findParentFoldersWithSubFolders(User user, @Param("parentFolderIds") List<Long> parentFolderIds);

    // 모든 상위폴더를 하위폴더와 함께 조회
    @Query("SELECT p FROM Folder p " +
            "LEFT JOIN FETCH p.subFolders " +
            "WHERE p.user = :user " +
            "AND p.parentFolder IS NULL " +
            "ORDER BY CASE WHEN p.markState = 'ACTIVE' THEN 0 ELSE 1 END, p.name ASC")
    Page<Folder> findAllFolders(User user, Pageable pageable);


    List<Folder> findTop4ByMarkStateAndUserOrderByMarkDateDesc(MarkStatus markState, User user);

}
