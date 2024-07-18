package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import org.aspectj.weaver.ast.Not;

import java.time.LocalDateTime;

public class NoteConverter {
    public static Note toWrite(NoteRequest.WriteDto request, Folder folder){
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
    public static NoteResponse.ShareResultDTO toShareResult(Note note){
        return NoteResponse.ShareResultDTO.builder()
                .uuid(note.getNoteUUID().toString())
                .build();
    }
    public static NoteResponse.SearchUUIDResultDTO toSearchUUIDResult(Note note){
        return NoteResponse.SearchUUIDResultDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .contents(note.getContents())
                .isEdit(note.getIsEdit())
                .build();
    }
}
