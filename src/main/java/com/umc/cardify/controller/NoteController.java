package com.umc.cardify.controller;

import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.FolderService;
import com.umc.cardify.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "NoteController", description = "노트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/notes")
public class NoteController {

    private final FolderService folderService;
    private final NoteService noteService;
    private final JwtUtil jwtUtil;
    @GetMapping("/addNote")
    @Operation(summary = "노트 추가 API")
    public ResponseEntity<NoteResponse.AddNoteResultDTO> addNote(@RequestHeader("Authorization") String token, @RequestParam @Valid Long folderId){
        Long userId = jwtUtil.extractUserId(token);
        Folder folder = folderService.getFolder(folderId);
        Note note = noteService.addNote(folder, userId);
        return ResponseEntity.ok(NoteConverter.toAddNoteResult(note));
    }
    @GetMapping("/share")
    @Operation(summary = "노트 공유 API" , description = "노트 아이디와 편집 여부 입력, 성공 시 uuid 반환(해당 uuid로 노트 특정)")
    public ResponseEntity<NoteResponse.ShareResultDTO> shareNote(@RequestHeader("Authorization") String token, @RequestParam @Valid Long noteId, @RequestParam @Valid Boolean isEdit){
        Long userId = jwtUtil.extractUserId(token);
        Note note = noteService.getNoteToID(noteId);
        note = noteService.shareNote(note, isEdit, userId);
        return ResponseEntity.ok(NoteConverter.toShareResult(note));
    }
    @PostMapping("/searchUUID")
    @Operation(summary = "공유한 노트 UUID로 탐색 API" , description = "노트 UUID 입력, 성공 시 노트 내용 반환")
    public ResponseEntity<NoteResponse.SearchUUIDResultDTO> searchUUIDNote(@RequestBody @Valid NoteRequest.SearchUUIDDto request){
        Note note = noteService.getNoteToUUID(request.getUuid());
        return ResponseEntity.ok(NoteConverter.toSearchUUIDResult(note));
    }
    @DeleteMapping("/deleteNote")
    @Operation(summary = "노트 삭제 API" , description = "노트 ID 입력, 성공 시 삭제 성공 여부 반환")
    public ResponseEntity<NoteResponse.IsSuccessNoteDTO> deleteNote(@RequestHeader("Authorization") String token, @RequestParam @Valid Long noteId){
        Long userId = jwtUtil.extractUserId(token);
        Boolean isSuccess = noteService.deleteNote(noteId, userId);
        return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
    }
    @PostMapping("/getNoteToFolder")
    @Operation(summary = "특정 폴더 내 노트 조회 API" ,
            description = "폴더 ID 입력, 성공 시 노트 리스트 반환 | order = asc, desc, edit-newest, edit-oldest |" +
                    " 페이지 번호, 사이즈 미입력시 페이징 X | 정렬방식 미입력시 이름 오름차순")
    public ResponseEntity<NoteResponse.GetNoteToFolderResultDTO> getNoteToFolder(@RequestBody @Valid NoteRequest.GetNoteToFolderDto request){
        Folder folder = folderService.getFolder(request.getFolderId());
        Page<Note> noteList = noteService.getNoteToFolder(folder, request);
        return ResponseEntity.ok(NoteConverter.toGetNoteToFolderResult(folder, noteList));
    }
    @GetMapping("/markNote")
    @Operation(summary = "노트 즐겨찾기 API" , description = "노트 ID와 즐겨찾기 여부 입력, 성공 시 즐겨찾기 성공 여부 반환")
    public ResponseEntity<NoteResponse.IsSuccessNoteDTO> markNote(@RequestHeader("Authorization") String token, @RequestParam @Valid Long noteId, @RequestParam @Valid Boolean isMark){
        Long userId = jwtUtil.extractUserId(token);
        Boolean isSuccess = noteService.markNote(noteId, isMark, userId);
        return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
    }
    @PostMapping("/write")
    @Operation(summary = "노트 작성 API" , description = "노트 UUID 입력, 성공 시 작성 성공 여부 반환")
    public ResponseEntity<NoteResponse.IsSuccessNoteDTO> writeNote(@RequestHeader("Authorization") String token, @RequestBody @Valid NoteRequest.WriteNoteDto request){
        Long userId = jwtUtil.extractUserId(token);
        Boolean isSuccess = noteService.writeNote(request, userId);
        return ResponseEntity.ok(NoteConverter.isSuccessNoteResult(isSuccess));
    }
}
