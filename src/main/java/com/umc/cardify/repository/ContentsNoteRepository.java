package com.umc.cardify.repository;

import com.umc.cardify.domain.ContentsNote;
import com.umc.cardify.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentsNoteRepository extends JpaRepository<ContentsNote, Long> {
    ContentsNote findByNote(Note note);
}
