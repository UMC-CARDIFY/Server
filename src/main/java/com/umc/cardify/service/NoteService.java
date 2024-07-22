package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteConverter noteConverter;

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

    public NoteResponse.NoteListDTO getNotesByUserId(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Note> notePage = noteRepository.findByUser(user, pageable);

        List<NoteResponse.NoteInfoDTO> notes = notePage.getContent().stream()
                .map(noteConverter::toNoteInfoDTO)
                .collect(Collectors.toList());

        return NoteResponse.NoteListDTO.builder()
                .noteList(notes)
                .listsize(notePage.getSize())
                .currentPage(notePage.getNumber()+1)
                .totalPage(notePage.getTotalPages())
                .totalElements(notePage.getTotalElements())
                .isFirst(notePage.isFirst())
                .isLast(notePage.isLast())
                .build();
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
