package com.umc.cardify.dto.folder;

import com.umc.cardify.domain.enums.MarkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class FolderResponse {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_01 : 폴더 정보 DTO")
    public static class FolderInfoDTO {
        @Schema(description = "폴더 아이디", example = "1")
        private Long folderId;
        @Schema(description = "폴더 이름", example = "Sample1")
        private String name;
        @Schema(description = "폴더 색상", example = "blue")
        private String color;
        @Schema(description = "폴더 즐겨찾기", example = "INACTIVE")
        private MarkStatus markState;
        @Schema(description = "폴더의 노트개수", example = "3")
        private Integer getNoteCount;
        @Schema(description = "폴더 즐겨찾기 수정 날짜", example = "2023-12-05 12:34:56")
        private Timestamp markDate;
        @Schema(description = "폴더 수정 날짜", example = "2023-12-05 12:34:56")
        private Timestamp editDate;
        @Schema(description = "폴더 생성 날짜", example = "2023-12-05 12:34:56")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_02 : 폴더 목록 응답 DTO")
    public static class FolderListDTO {
        @Schema(description = "폴더 목록")
        private List<FolderInfoDTO> foldersList;
        @Schema(description = "리스트 사이즈", example = "10")
        private Integer listSize;
        @Schema(description = "현재 페이지 번호", example = "1")
        private Integer currentPage;
        @Schema(description = "총 페이지 수", example = "5")
        private Integer totalPages;
        @Schema(description = "총 폴더 수", example = "5")
        private Long totalElements;
        @Schema(description = "첫 페이지인지 확인", example = "true")
        private Boolean isFirst;
        @Schema(description = "마지막 페이지인지 확인", example = "false")
        private Boolean isLast;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_03 : 폴더 정렬 정보 DTO")
    public static class sortFolderInfoDTO {
        @Schema(description = "폴더 아이디", example = "1")
        private Long folderId;
        @Schema(description = "폴더 이름", example = "Sample1")
        private String name;
        @Schema(description = "폴더 수정 날짜", example = "2023-12-05 12:34:56")
        private Timestamp editDate;
        @Schema(description = "폴더 생성 날짜", example = "2023-12-05 12:34:56")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_04 : 폴더 정렬 응답 DTO")
    public static class sortFolderListDTO {
        @Schema(description = "폴더 정렬 목록")
        private List<sortFolderInfoDTO> sortFoldersList;
        @Schema(description = "리스트 사이즈", example = "10")
        private Integer listSize;
        @Schema(description = "현재 페이지 번호", example = "1")
        private Integer currentPage;
        @Schema(description = "총 페이지 수", example = "5")
        private Integer totalPages;
        @Schema(description = "총 폴더 수", example = "5")
        private Long totalElements;
        @Schema(description = "첫 페이지인지 확인", example = "true")
        private Boolean isFirst;
        @Schema(description = "마지막 페이지인지 확인", example = "false")
        private Boolean isLast;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_05 : 폴더 삭제 응답 DTO")
    public static class deleteFolderResultDTO{
        @Schema(description = "삭제 성공 여부", example = "true")
        Boolean isSuccess;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_06 : 폴더 추가 응답 DTO")
    public static class addFolderResultDTO{
        @Schema(description = "폴더 아이디", example = "1")
        Long folderId;
        @Schema(description = "폴더 이름", example = "sample")
        String name;
        @Schema(description = "폴더 색상", example = "blue")
        String color;
        @Schema(description = "폴더 생성 날짜", example = "2023-12-05 12:34:56")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_07 : 폴더 수정 응답 DTO")
    public static class editFolderResultDTO {
        @Schema(description = "폴더 아이디", example = "1")
        Long folderId;
        @Schema(description = "폴더 이름", example = "sample")
        String name;
        @Schema(description = "폴더 색상", example = "ocean")
        String color;
        @Schema(description = "폴더 수정 날짜", example = "2023-12-05 12:34:56")
        Timestamp editDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_08 : 폴더 즐겨찾기 응답 DTO")
    public static class markFolderResultDTO{
        @Schema(description = "즐겨찾기 성공 여부", example = "true")
        Boolean isSuccess;
        @Schema(description = "즐겨찾기 상태", example = "ACTIVE")
        MarkStatus markState;
        @Schema(description = "폴더 즐겨찾기 수정 날짜", example = "2023-12-05 12:34:56")
        Timestamp markDate;
    }

}
