package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import org.aspectj.weaver.ast.Not;

import java.time.LocalDateTime;

public class NoteConverter {
    public static Note toWrite(NoteRequest.writeDto request, Folder folder){
        return Note.builder()
                .folder(folder)
                .name(request.getName())
                .contents(request.getContents())
                .build();
    }
    public static NoteResponse.WriteResultDTO toWriteResult(Note note){
        return NoteResponse.WriteResultDTO.builder()
                .noteId(note.getNoteId())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
