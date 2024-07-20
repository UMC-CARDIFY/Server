package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.repository.NoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    public Note getNoteToID(long noteId){
        return noteRepository.findById(noteId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
    }
    public Note getNoteToUUID(String uuid_str){
        try{
            UUID uuid = UUID.fromString(uuid_str);
            Note note = noteRepository.findByNoteUUID(uuid);
            if(note == null)
                throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR);
            return note;
        }catch (IllegalArgumentException e){
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }
    }
    public Note writeNote(NoteRequest.WriteDto request, Folder folder){
        Note newNote = NoteConverter.toWrite(request, folder);
        return noteRepository.save(newNote);
    }

    public Page<Note> getAllNotes(NoteRequest.getAllDto request, Pageable pageable) {
        return noteRepository.findAll(pageable);
    }
    public Note shareNote(Note note, Boolean isEdit){
        if(note.getNoteUUID().equals(null)){
            note.setNoteUUID(UUID.randomUUID());
        }
        note.setIsEdit(isEdit);
        return noteRepository.save(note);
    }
    public Boolean deleteNote(NoteRequest.DeleteNoteDto request){
        Note note_del = noteRepository.findById(request.getNoteId()).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        noteRepository.delete(note_del);
        return true;
    }
}
