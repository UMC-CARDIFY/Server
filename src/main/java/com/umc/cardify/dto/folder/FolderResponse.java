package com.umc.cardify.dto.folder;

import com.umc.cardify.domain.enums.MarkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class FolderResponse {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_01 : 폴더 정보 DTO")
    public static class FolderInfoDTO {
        @Schema(description = "폴더 아이디", example = "1")
        Long folderId;
        @Schema(description = "폴더 이름", example = "Sample1")
        String name;
        @Schema(description = "폴더 색상", example = "red")
        String color;
        @Schema(description = "폴더 즐겨찾기", example = "ACTIVE")
        MarkStatus markState;
        @Schema(description = "폴더의 노트개수", example = "3")
        Integer getNoteCount;
        @Schema(description = "폴더 수정 날짜", example = "2023-12-05T12:34:56")
        Timestamp editDate;
        @Schema(description = "폴더 생성 날짜", example = "2023-12-05T12:34:56")
        LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_02 : 폴더 목록 응답 DTO")
    public static class FolderListDTO {
        @Schema(description = "폴더 목록")
        List<FolderInfoDTO> foldersList;
        @Schema(description = "리스트 사이즈", example = "10")
        Integer listSize;
        @Schema(description = "현재 페이지 번호", example = "1")
        Integer currentPage;
        @Schema(description = "총 페이지 수", example = "5")
        Integer totalPages;
        @Schema(description = "총 폴더 수", example = "5")
        Long totalElements;
        @Schema(description = "첫 페이지인지 확인", example = "true")
        Boolean isFirst;
        @Schema(description = "마지막 페이지인지 확인", example = "false")
        Boolean isLast;
    }
}
