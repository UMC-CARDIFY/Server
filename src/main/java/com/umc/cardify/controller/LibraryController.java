package com.umc.cardify.controller;

import com.umc.cardify.dto.library.LibraryRequest;
import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "LibraryController", description = "자료실 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/library")
public class LibraryController {

    private final JwtUtil jwtUtil;
    private final LibraryService libraryService;
    @GetMapping("/getCategory")
    @Operation(summary = "카테고리 조회 API")
    public ResponseEntity<List<LibraryResponse.CategoryInfoDTO>> getCategory(){
        List<LibraryResponse.CategoryInfoDTO> resultCategory = libraryService.getCategory();
        return ResponseEntity.ok(resultCategory);
    }
    @PostMapping("/download")
    @Operation(summary = "자료실 다운로드 API")
    public ResponseEntity<LibraryResponse.IsSuccessLibDTO> downloadLib(@RequestHeader("Authorization") String token, @RequestBody @Valid LibraryRequest.DownloadLibDto request){
        Long userId = jwtUtil.extractUserId(token);
        Boolean isSuccess = libraryService.downloadLib(userId, request);
        return ResponseEntity.ok(LibraryResponse.IsSuccessLibDTO.builder().isSuccess(isSuccess).build());
    }
    @GetMapping("/getTopNote")
    @Operation(summary = "추천 노트 조회 API")
    public ResponseEntity<List<LibraryResponse.LibInfoDTO>> getTopNote(@RequestHeader("Authorization") String token){
        Long userId = jwtUtil.extractUserId(token);
        List<LibraryResponse.LibInfoDTO> resultNote = libraryService.getTopNote(userId);
        int index = 3;
        if(resultNote.size() < index)
            index = resultNote.size();

        return ResponseEntity.ok(resultNote.subList(0, index));
    }
    @GetMapping("/getTopCategory")
    @Operation(summary = "추천 카테고리 조회 API")
    public ResponseEntity<List<LibraryResponse.CategoryInfoDTO>> getTopCategory(){
        List<LibraryResponse.CategoryInfoDTO> resultCategory = libraryService.getTopCategory()
                .stream().sorted(Comparator.comparing(LibraryResponse.CategoryInfoDTO::getCntNote).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultCategory.subList(0, 3));
    }
    @GetMapping("/getNoteToCategory")
    @Operation(summary = "특정 카테고리 내 노트 조회 API",
            description = "order = asc, desc, upload-newest, upload-oldest, download | 정렬방식 미입력시 이름 오름차순")
    public ResponseEntity<List<LibraryResponse.LibInfoDTO>> getNoteToCategory(@RequestHeader("Authorization") String token, @RequestParam @Valid String category, @RequestParam @Valid String order){
        Long userId = jwtUtil.extractUserId(token);
        List<LibraryResponse.LibInfoDTO> resultNote = libraryService.getNoteToCategory(category, order, userId);
        return ResponseEntity.ok(resultNote);
    }
    @PostMapping("/searchLib")
    @Operation(summary = "자료실 내 노트 검색 API")
    public ResponseEntity<LibraryResponse.SearchLibDTO> searchLib(@RequestHeader("Authorization") String token, @RequestBody @Valid LibraryRequest.SearchLibDto request){
        Long userId = jwtUtil.extractUserId(token);
        LibraryResponse.SearchLibDTO resultDto = libraryService.searchLib(request, userId);
        return ResponseEntity.ok(resultDto);
    }
}
