package com.umc.cardify.service;

import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    public Note writeNote(NoteRequest.writeDto request, Folder folder){
        Note newNote = NoteConverter.toWrite(request, folder);
        return noteRepository.save(newNote);
    }

    public Page<Note> getAllNotes(NoteRequest.getAllDto request, Pageable pageable) {
        return noteRepository.findAll(pageable);
    }
}
