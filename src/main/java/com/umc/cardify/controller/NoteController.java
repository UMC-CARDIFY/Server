package com.umc.cardify.controller;

import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "NoteController", description = "노트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/notes")
public class NoteController {

    private final FolderService folderService;
    private final NoteService noteService;
    @PostMapping("/write")
    @Operation(summary = "노트 추가 API")
    public ResponseEntity<NoteResponse.WriteResultDTO> writeNote(@RequestBody @Valid NoteRequest.writeDto request){
        Folder folder = folderService.getFolder(request.getFolderId());
        Note note = noteService.writeNote(request, folder);
        return ResponseEntity.ok(NoteConverter.toWriteResult(note));
    }
}