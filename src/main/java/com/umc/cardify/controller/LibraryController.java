package com.umc.cardify.controller;

import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "LibraryController", description = "자료실 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/Library")
public class LibraryController {

    private final JwtUtil jwtUtil;
    private final LibraryService libraryService;
    @GetMapping("/getCategory")
    @Operation(summary = "카테고리 조회 API")
    public ResponseEntity<List<LibraryResponse.LibraryInfoDTO>> getCategory(){
        List<LibraryResponse.LibraryInfoDTO> resultCategory = libraryService.getCategory();
        return ResponseEntity.ok(resultCategory);
    }
}
