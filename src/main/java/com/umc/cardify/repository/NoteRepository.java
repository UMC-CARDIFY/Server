package com.umc.cardify.repository;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, Long> {
    Note findByNoteUUID(UUID uuid);
    List<Note> findByFolder(Folder folder);
    @Query("SELECT n FROM Note n WHERE n.folder.user = :user")
    Page<Note> findByUser(@Param("user") User user, Pageable pageable);
}
