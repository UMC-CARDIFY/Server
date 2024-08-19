package com.umc.cardify.controller;

import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.umc.cardify.jwt.JwtUtil;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "폴더 목록 조회 API", description = "조회 성공 시, 해당 유저의 폴더 목록 반환 | page, size는 수정 가능")
    public ResponseEntity<FolderResponse.FolderListDTO> getAllFolders(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.FolderListDTO folders = folderService.getFoldersByUserId(userId, page, size);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/sort")
    @Operation(summary = "폴더 정렬 기능 API", description = "성공 시 해당 유저의 폴더를 정렬해서 반환 | order = asc, desc, edit-newest, edit-oldest")
    public ResponseEntity<FolderResponse.FolderListDTO> sortFolders(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false)  Integer page,
            @RequestParam(required = false)  Integer size,
            @RequestParam String order){
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.FolderListDTO folders = folderService.sortFoldersByUserId(userId, page, size, order);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/notes/sort")
    @Operation(summary = "노트 정렬 기능 API", description = "성공 시 해당 유저의 전체 노트를 정렬해서 반환(폴더 상관없이 전체 노트 정렬) | order = asc, desc, edit-newest, edit-oldest")
    public ResponseEntity<NoteResponse.NoteListDTO> sortNotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false)  Integer page,
            @RequestParam(required = false)  Integer size,
            @RequestParam String order){
        Long userId = jwtUtil.extractUserId(token);
        NoteResponse.NoteListDTO notes = noteService.sortNotesByUserId(userId, page, size, order);
        return ResponseEntity.ok(notes);
    }


    @GetMapping("/notes")
    @Operation(summary = "노트 목록 조회 API", description = "조회 성공 시, 해당 유저의 노트 목록 반환(폴더 상관없이 전체 노트) | 페이징 제한 없음")
    public ResponseEntity<NoteResponse.NoteListDTO> getAllNotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false)  Integer page,
            @RequestParam(required = false)  Integer size) {
        Long userId = jwtUtil.extractUserId(token);
        NoteResponse.NoteListDTO notes = noteService.getNotesByUserId(userId, page, size);
        return ResponseEntity.ok(notes);
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "특정 폴더 삭제 API", description = "해당 유저의 특정 폴더 삭제 성공 시, true 응답 반환 | 삭제를 원하는 folderId 입력")
    public ResponseEntity<FolderResponse.deleteFolderResultDTO> deleteFolder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long folderId) {
        Long userId = jwtUtil.extractUserId(token);
        folderService.deleteFolderById(userId, folderId);
        return ResponseEntity.ok(FolderResponse.deleteFolderResultDTO.builder().isSuccess(true).build());
    }


    @PostMapping("/addFolder")
    @Operation(summary = "폴더 추가 기능 API", description = "해당 유저의 폴더를 생성 시, 폴더 아이디, 이름, 색상, 생성일 반환 | 이름,색상 입력")
    public ResponseEntity<FolderResponse.addFolderResultDTO> addFolder(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid FolderRequest.addFolderDto folderRequest) {
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.addFolderResultDTO response = folderService.addFolder(userId, folderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PatchMapping("/{folderId}")
    @Operation(summary = "폴더 수정 기능 API", description = "해당 유저의 폴더를 수정 시, 수정된 이름과 색상, 수정일을 반환")
    public ResponseEntity<FolderResponse.editFolderResultDTO> editFolder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long folderId,
            @RequestBody @Valid FolderRequest.editFolderDto folderRequest) {
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.editFolderResultDTO response = folderService.editFolder(userId, folderId, folderRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{folderId}/mark-folders")
    @Operation(summary = "특정 폴더 즐겨찾기 기능 API", description = "해당 유저의 특정 폴더를 즐겨찾기 시, 폴더의 markState 값에 따라서 ACTIVE/INACTIVE로 변경")
    public ResponseEntity<FolderResponse.markFolderResultDTO> markFolder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long folderId) {
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.markFolderResultDTO response = folderService.markFolderById(userId, folderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    @Operation(summary = "폴더 필터링 기능 API", description = "해당 유저의 폴더를 색상으로 필터링하여 반환 | 쉼표로 구분된 색상 문자열 입력")
    public ResponseEntity<FolderResponse.FolderListDTO> filterFolders(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam String color) {
        Long userId = jwtUtil.extractUserId(token);
        FolderResponse.FolderListDTO folders = folderService.filterColorsByUserId(userId, page, size, color);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/notes/filter")
    @Operation(summary = "노트 필터링 기능 API", description = "사용자의 노트를 특정 색상으로 필터링하여 반환 | 쉼표로 구분된 색상 문자열 입력")
    public ResponseEntity<NoteResponse.NoteListDTO> filterNotes(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam String color) {
        Long userId = jwtUtil.extractUserId(token);
        NoteResponse.NoteListDTO notes = noteService.filterColorsNotes(userId, page, size, color);
        return ResponseEntity.ok(notes);
    }
}