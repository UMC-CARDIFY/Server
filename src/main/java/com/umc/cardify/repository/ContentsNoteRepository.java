package com.umc.cardify.repository;

import com.umc.cardify.domain.ContentsNote;
import com.umc.cardify.domain.Note;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContentsNoteRepository extends MongoRepository<ContentsNote, Long> {
    ContentsNote findByNoteId(Long noteId);
}
