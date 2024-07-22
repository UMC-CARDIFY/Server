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
@RequestMapping("api/v1/notes")
public class NoteController {

    private final FolderService folderService;
    private final NoteService noteService;
    @PostMapping("/write")
    @Operation(summary = "노트 추가 API")
    public ResponseEntity<NoteResponse.WriteResultDTO> writeNote(@RequestBody @Valid NoteRequest.WriteDto request){
        Folder folder = folderService.getFolder(request.getFolderId());
        Note note = noteService.writeNote(request, folder);
        return ResponseEntity.ok(NoteConverter.toWriteResult(note));
    }
    @PostMapping("/share")
    @Operation(summary = "노트 공유 API" , description = "노트 아이디와 편집 여부 입력, 성공 시 uuid 반환(해당 uuid로 노트 특정)")
    public ResponseEntity<NoteResponse.ShareResultDTO> shareNote(@RequestBody @Valid NoteRequest.ShareDto request){
        Note note = noteService.getNoteToID(request.getNoteId());
        note = noteService.shareNote(note, request.getIsEdit());
        return ResponseEntity.ok(NoteConverter.toShareResult(note));
    }
    @PostMapping("/searchUUID")
    @Operation(summary = "공유한 노트 UUID로 탐색 API" , description = "노트 UUID 입력, 성공 시 노트 내용 반환")
    public ResponseEntity<NoteResponse.SearchUUIDResultDTO> searchUUIDNote(@RequestBody @Valid NoteRequest.SearchUUIDDto request){
        Note note = noteService.getNoteToUUID(request.getUuid());
        return ResponseEntity.ok(NoteConverter.toSearchUUIDResult(note));
    }
    @PostMapping("/deleteNote")
    @Operation(summary = "노트 삭제 API" , description = "노트 ID 입력, 성공 시 삭제 성공 여부 반환")
    public ResponseEntity<NoteResponse.deleteNoteResultDTO> deleteNote(@RequestBody @Valid NoteRequest.DeleteNoteDto request){
        Boolean isSuccess = noteService.deleteNote(request);
        return ResponseEntity.ok(NoteConverter.toDeleteNoteResult(isSuccess));
    }
}
