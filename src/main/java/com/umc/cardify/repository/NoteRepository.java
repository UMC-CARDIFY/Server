package com.umc.cardify.repository;

import com.umc.cardify.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, Long> {
    Note findByNoteUUID(UUID uuid);
}
