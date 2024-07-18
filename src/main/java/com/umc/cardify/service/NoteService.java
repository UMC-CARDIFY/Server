package com.umc.cardify.service;

import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    public Note getNote(long noteId){
        return noteRepository.getById(noteId);
    }
    public Note getNote(UUID uuid){ return noteRepository.findByNoteUUID(uuid); }
    public Note writeNote(NoteRequest.WriteDto request, Folder folder){
        Note newNote = NoteConverter.toWrite(request, folder);
        return noteRepository.save(newNote);
    }
    public Note shareNote(Note note, Boolean isEdit){
        if(note.getNoteUUID().equals(null)){
            note.setNoteUUID(UUID.randomUUID());
        }
        note.setIsEdit(isEdit);
        return noteRepository.save(note);
    }
}
