package com.umc.cardify.controller;

import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @GetMapping("/sort")
    @Operation(summary = "폴더 정렬 기능 API", description = "해당 유저의 폴더를 정렬해서 반환, 페이징을 포함 query string으로 페이지 번호를 주세요. | order = asc, desc, edit-newest, edit-oldest")
    public ResponseEntity<FolderResponse.sortFolderListDTO> sortFolders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam int size,
            @RequestParam String order){
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.sortFolderListDTO folders = folderService.sortFoldersByUserId(userId, page, size, order);
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

    @DeleteMapping("/{folderId}")
    @Operation(summary = "특정 폴더 삭제 API", description = "해당 유저의 특정 폴더 삭제 성공 시, true 응답 반환 | 삭제를 원하는 folderId 입력")
    public ResponseEntity<FolderResponse.deleteFolderResultDTO> deleteFolder(
            @RequestHeader("Authorization") String token,
            @RequestParam Long folderId) {
        Long userId = jwtUtil.extractUserId(token);
        folderService.deleteFolderById(userId, folderId);
        return ResponseEntity.ok(FolderResponse.deleteFolderResultDTO.builder().isSuccess(true).build());
    }
}
