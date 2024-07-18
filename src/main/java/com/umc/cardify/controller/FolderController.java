package com.umc.cardify.controller;

import com.umc.cardify.converter.FolderConverter;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FolderController", description = "폴더 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/folders")
public class FolderController {

    private final FolderService folderService;
    private final NoteService noteService;

    @GetMapping
    @Operation(summary = "모든 폴더 조회 API")
    public ResponseEntity<FolderResponse.GetAllFolderResultDTO> getAllFolders(
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 5); // 페이지 크기를 5로 고정, 페이지는 0부터 시작
        Page<Folder> folders = folderService.getAllFolders(folderId, pageable);
        return ResponseEntity.ok(FolderConverter.toGetAllResult(folders));
    }


    @PostMapping("/notes")
    @Operation(summary = "모든 노트 조회 API")
    public ResponseEntity<NoteResponse.GetAllResultDTO> getAllNotes(
            @RequestBody @Valid NoteRequest.getAllDto request, @RequestParam(defaultValue = "0") int page){
        Pageable pageable = PageRequest.of(page, 5);
        Page<Note> notes = noteService.getAllNotes(request, pageable);
        return ResponseEntity.ok(NoteConverter.toGetAllResult(notes));
    }


}
