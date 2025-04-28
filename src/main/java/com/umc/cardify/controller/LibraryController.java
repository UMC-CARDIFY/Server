package com.umc.cardify.controller;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.dto.library.LibraryRequest;
import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.repository.UserRepository;
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

    private final LibraryService libraryService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping("/getCategory")
    @Operation(summary = "카테고리 조회 API")
    public ResponseEntity<List<LibraryResponse.CategoryInfoDTO>> getCategory(){
        List<LibraryResponse.CategoryInfoDTO> resultCategory = libraryService.getCategory();
        return ResponseEntity.ok(resultCategory);
    }
    @PostMapping("/download")
    @Operation(summary = "자료실 다운로드 API")
    public ResponseEntity<LibraryResponse.DownloadLibDTO> downloadLib(@RequestHeader("Authorization") String token, @RequestBody @Valid LibraryRequest.DownloadLibDto request){
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        Long userId = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

        LibraryResponse.DownloadLibDTO dto = libraryService.downloadLib(userId, request);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/getTopNote")
    @Operation(summary = "추천 노트 조회 API")
    public ResponseEntity<List<LibraryResponse.LibInfoDTO>> getTopNote(@RequestHeader("Authorization") String token, @RequestParam @Valid Integer size){
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        Long userId = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

        List<LibraryResponse.LibInfoDTO> resultNote = libraryService.getTopNote(userId);
        if(resultNote.size() < size)
            size = resultNote.size();

        return ResponseEntity.ok(resultNote.subList(0, size));
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
            description = "order = asc, desc, upload-newest, upload-oldest, download")
    public ResponseEntity<List<LibraryResponse.LibInfoDTO>> getNoteToCategory(@RequestHeader("Authorization") String token, @RequestParam @Valid String category, @RequestParam @Valid String order){
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        Long userId = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

        List<LibraryResponse.LibInfoDTO> resultNote = libraryService.getNoteToCategory(category, order, userId);
        return ResponseEntity.ok(resultNote);
    }
    @PostMapping("/searchLib")
    @Operation(summary = "자료실 내 노트 검색 API", description = "카테고리 미입력시 전체 조회")
    public ResponseEntity<LibraryResponse.SearchLibDTO> searchLib(@RequestHeader("Authorization") String token, @RequestBody @Valid LibraryRequest.SearchLibDto request){
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        Long userId = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

        LibraryResponse.SearchLibDTO resultDto = libraryService.searchLib(request, userId);
        return ResponseEntity.ok(resultDto);
    }
    @GetMapping("/checkDownload")
    @Operation(summary = "자료실 노트 다운로드 방식 조회 API")
    public ResponseEntity<LibraryResponse.CheckDownloadDTO> checkDownload(@RequestHeader("Authorization") String token, @RequestParam @Valid Long libraryId){
        String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
        AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함
        Long userId = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();

        LibraryResponse.CheckDownloadDTO checkDto = libraryService.checkDownload(userId, libraryId);
        return ResponseEntity.ok(checkDto);
    }
}
