package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
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

    public NoteResponse.NoteInfoDTO toNoteInfoDTO(Note note) {
        return NoteResponse.NoteInfoDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .editDate(note.getEditDate())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
