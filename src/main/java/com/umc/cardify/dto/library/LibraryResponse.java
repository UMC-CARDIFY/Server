package com.umc.cardify.dto.library;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class LibraryResponse {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_01 : 카테고리 조회 DTO")
    public static class CategoryInfoDTO {
        Long categoryId;
        String categoryName;
        Integer cntNote;
        LocalDateTime uploadAt;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_02 : API 실행 성공 여부 응답 DTO")
    public static class DownloadLibDTO {
        Long noteId;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_03 : 각종 노트 조회 DTO")
    public static class LibInfoDTO {
        Long libraryId;
        Long noteId;
        String noteName;
        List<String> categoryName;
        Integer cntCard;
        String userImgSrc;
        String userName;
        Boolean isDownload;
        Integer cntDownloadAll;
        Integer cntDownloadWeek;
        LocalDateTime uploadAt;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_04 : 추천 노트 조회 DTO")
    public static class SearchLibDTO {
        String searchTxt;
        List<String> searchCategory;
        List<LibInfoDTO> resultNote;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_05 : 다운로드 방식 조회 DTO")
    public static class CheckDownloadDTO {
        Long noteId;
        String isDownload;
    }
}
