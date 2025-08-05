package com.umc.cardify.repository;

import com.umc.cardify.domain.Note;
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
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByFolder(Folder folder);

    @Query("SELECT n FROM Note n WHERE n.folder.user = :user ORDER BY " +
           "CASE WHEN n.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN n.markState = 'ACTIVE' THEN n.markAt END ASC, " +
            "CASE WHEN n.markState != 'ACTIVE' THEN n.createdAt END DESC")
    Page<Note> findByUser(@Param("user") User user, Pageable pageable); // 전체 노트 조회 - 아카이브
    void deleteByFolder(Folder folder);

    Note findTopByFolderOrderByEditDateDesc(Folder folder);

    @Query("SELECT n FROM Note n WHERE n.folder.user = :user AND n.folder.color IN (:colors) ORDER BY " +
            "CASE WHEN n.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "CASE WHEN n.markState = 'ACTIVE' THEN n.markAt END ASC, " +
            "CASE WHEN n.markState != 'ACTIVE' THEN n.createdAt END DESC")
    Page<Note> findByNoteIdAndUser(@Param("user") User user, @Param("colors") List<String> colorList, Pageable pageable); // 전체 노트 필터링 - 아카이브

    @Query("SELECT n FROM Note n WHERE n.folder.user = :user ORDER BY " +
            "CASE WHEN n.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "n.markAt ASC, " +
            "CASE WHEN :order = 'asc' THEN n.name END ASC, " +
            "CASE WHEN :order = 'edit-oldest' THEN n.editDate END ASC, " +
            "CASE WHEN :order = 'desc' THEN n.name END DESC, " +
            "CASE WHEN :order = 'edit-newest' THEN n.editDate END DESC")
    Page<Note> findByUserAndSort(@Param("user") User user, @Param("order") String order, Pageable pageable); // 전체 노트 정렬 - 아카이브

    @Query("SELECT n FROM Note n WHERE n.folder.user = :user AND n.folder.color IN (:colors) ORDER BY " +
            "CASE WHEN n.markState = 'ACTIVE' THEN 0 ELSE 1 END, " +
            "n.markAt ASC, " +
            "CASE WHEN :order = 'asc' THEN n.name END ASC, " +
            "CASE WHEN :order = 'edit-oldest' THEN n.editDate END ASC, " +
            "CASE WHEN :order = 'desc' THEN n.name END DESC, " +
            "CASE WHEN :order = 'edit-newest' THEN n.editDate END DESC")
    Page<Note> findByUserColorAndSort(@Param("user") User user, @Param("colors") List<String> colorList, @Param("order") String order, Pageable pageable); // 전체 노트 정렬&필터링 동시 사용 - 아카이브

    @Query("SELECT n FROM Note n WHERE n.folder.user = :user ORDER BY " +
            "n.viewAt DESC ")
    Page<Note> findByUserOrderByViewAtDesc(User user, Pageable pageable);


    Optional<Note> findByUuid(String UUID);

    Page<Note> findByFolder(Folder folder, Pageable pageable);

    // 정렬 조건으로 모든 노트 조회
    List<Note> findByFolder(Folder folder, Sort sort);

    // 기존 카드 개수 조회 메서드
    @Query("SELECT n.noteId, COUNT(c.cardId) " +
            "FROM Note n LEFT JOIN n.cards c " +
            "WHERE n.folder = :folder " +
            "GROUP BY n.noteId")
    List<Object[]> findNoteCardCounts(@Param("folder") Folder folder);
}
