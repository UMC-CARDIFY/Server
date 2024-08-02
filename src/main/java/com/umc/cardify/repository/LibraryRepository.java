package com.umc.cardify.repository;

import com.umc.cardify.domain.Library;
import com.umc.cardify.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRepository extends JpaRepository<Library, Long> {
    Library findByNote(Note note);
}
