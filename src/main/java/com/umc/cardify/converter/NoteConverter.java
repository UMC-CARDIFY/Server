package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    public static NoteResponse.GetAllResultDTO toGetAllResult(Page<Note> notePage) {
        List<NoteResponse.NoteDTO> noteDTOs = notePage.getContent().stream()
                .map(note -> NoteResponse.NoteDTO.builder()
                        .noteId(note.getNoteId())
                        .name(note.getName())
                        .contents(note.getContents())
                        .createdAt(note.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return NoteResponse.GetAllResultDTO.builder()
                .notes(noteDTOs)
                .currentPage(notePage.getNumber())
                .totalPages(notePage.getTotalPages())
                .totalElements(notePage.getTotalElements())
                .build();
    }
}
