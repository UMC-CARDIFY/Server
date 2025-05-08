package com.umc.cardify.dto.folder;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        @Schema(description = "폴더 즐겨찾기 수정 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd")
        private Timestamp markDate;
        @Schema(description = "폴더 수정 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd")
        private Timestamp editDate;
        @Schema(description = "폴더 생성 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd", timezone = "Asia/Seoul")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_02 : 폴더 목록 응답 DTO")
    public static class FolderListDTO {
        // FIXME : 부모 폴더 내의 폴더 조회시 부모 폴더 이름 및 색상 추가로 응답해야 함
        @Schema(description = "부모 폴더 이름")
        private String parentFolderName;
        @Schema(description = "부모 폴더 색상")
        private String parentFolderColor;
        @Schema(description = "폴더 목록")
        private List<FolderInfoDTO> foldersList;
        @Schema(description = "리스트 사이즈", example = "10")
        private Integer listSize;
        @Schema(description = "현재 페이지 번호", example = "1")
        private Integer currentPage;
        @Schema(description = "총 페이지 수", example = "5")
        private Integer totalPages;
        @Schema(description = "총 폴더 수", example = "5")
        private Integer totalElements;
        @Schema(description = "첫 페이지인지 확인", example = "true")
        private Boolean isFirst;
        @Schema(description = "마지막 페이지인지 확인", example = "false")
        private Boolean isLast;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_03 : 폴더 삭제 응답 DTO")
    public static class deleteFolderResultDTO{
        @Schema(description = "삭제 성공 여부", example = "true")
        private Boolean isSuccess;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_04 : 폴더 추가 응답 DTO")
    public static class addFolderResultDTO{
        @Schema(description = "상위 폴더 아이디", example = "1")
        private Long parent_folderId;
        @Schema(description = "폴더 아이디", example = "1")
        private Long folderId;
        @Schema(description = "폴더 이름", example = "sample")
        private String name;
        @Schema(description = "폴더 색상", example = "blue")
        private String color;
        @Schema(description = "폴더 생성 날짜", example = "2023/12/05")
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_05 : 폴더 수정 응답 DTO")
    public static class editFolderResultDTO {
        @Schema(description = "폴더 아이디", example = "1")
        Long folderId;
        @Schema(description = "폴더 이름", example = "sample")
        String name;
        @Schema(description = "폴더 색상", example = "ocean")
        String color;
        @Schema(description = "폴더 수정 날짜", example = "2023/12/05")
        Timestamp editDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_06 : 폴더 즐겨찾기 응답 DTO")
    public static class markFolderResultDTO{
        @Schema(description = "즐겨찾기 성공 여부", example = "true")
        private Boolean isSuccess;
        @Schema(description = "즐겨찾기 상태", example = "ACTIVE")
        private MarkStatus markState;
        @Schema(description = "폴더 즐겨찾기 수정 날짜", example = "2023-12-05 12:34:56")
        private Timestamp markDate;
    }

}
