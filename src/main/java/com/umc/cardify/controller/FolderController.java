package com.umc.cardify.controller;

import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.umc.cardify.jwt.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FolderController", description = "폴더 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/folders")
public class FolderController {

    private final FolderService folderService;
    private final NoteService noteService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "폴더 목록 조회 API", description = "조회 성공 시, 해당 유저의 폴더 목록 반환")
    public ResponseEntity<FolderResponse.FolderListDTO> getAllFolders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam int size) {
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.FolderListDTO folders = folderService.getFoldersByUserId(userId, page, size);
        return ResponseEntity.ok(folders);
    }


    @GetMapping("/notes")
    @Operation(summary = "노트 조회", description = "조회 성공 시, 해당 유저의 노트목록 반환")
    public ResponseEntity<NoteResponse.NoteListDTO> getAllNotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam int size) {
        Long userId = jwtUtil.extractUserId(token);
        NoteResponse.NoteListDTO notes = noteService.getNotesByUserId(userId, page, size);
        return ResponseEntity.ok(notes);
    }
}
