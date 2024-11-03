package com.umc.cardify.repository;

import com.umc.cardify.domain.ContentsNote;
import com.umc.cardify.domain.Note;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ContentsNoteRepository extends MongoRepository<ContentsNote, ObjectId> {
    Optional<ContentsNote> findByNoteId(Long noteId);
}
