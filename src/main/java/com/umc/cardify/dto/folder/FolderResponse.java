package com.umc.cardify.dto.folder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.note.NoteResponse;
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
        @Schema(description = "부모 폴더 이름")
        private String parentFolderName;
        @Schema(description = "부모 폴더 색상")
        private String parentFolderColor;
        @Schema(description = "폴더 즐겨찾기", example = "INACTIVE")
        private MarkStatus parentMarkState;
        @Schema(description = "폴더의 노트개수", example = "3")
        private Integer getNoteCount;
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
        @JsonFormat(pattern= "yy/MM/dd")
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

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_07 : 하위폴더 이동 - 상위 폴더 정보 DTO")
    public static class ParentFolderInfoDTO {
        @Schema(description = "상위 폴더 아이디", example = "1")
        private Long folderId;
        @Schema(description = "상위 폴더 이름", example = "Sample1")
        private String folderName;
        @Schema(description = "상위 폴더 색상", example = "blue")
        private String folderColor;
        @Schema(description = "상위 폴더 즐겨찾기", example = "INACTIVE")
        private MarkStatus markState;
        @Schema(description = "상위 폴더의 하위폴더 개수", example = "3")
        private Integer getSubFolderCount;
        @Schema(description = "상위 폴더 즐겨찾기 수정 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd")
        private Timestamp markDate;
        @Schema(description = "상위 폴더 수정 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd")
        private Timestamp editDate;
        @Schema(description = "상위 폴더 생성 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd", timezone = "Asia/Seoul")
        private LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_08 : 하위폴더 이동 - 상위 폴더 목록 DTO")
    public static class ParentFolderListDTO {
        @Schema(description = "상위 폴더 목록", example = "[]")
        private List<ParentFolderInfoDTO> parentFolders;
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
    @Schema(title = "FOLDER_RES_09 : 하위폴더 이동 - 이동결과 DTO")
    public static class FolderMoveResultDTO  {
        @Schema(description = "이동할 하위 폴더 ID", example = "1")
        private Long folderId;
        @Schema(description = "이동할 하위 폴더 이름", example = "test")
        private String folderName;
        @Schema(description = "이전 소속 상위 폴더 ID", example = "2")
        private Long previousParentFolderId;
        @Schema(description = "이전 소속 상위 폴더 이름", example = "sample")
        private String previousParentFolderName;
        @Schema(description = "새로운 소속 상위 폴더 ID", example = "1")
        private Long newParentFolderId;
        @Schema(description = "새로운 소속 상위 폴더 이름", example = "test")
        private String newParentFolderName;
        @Schema(description = "새로운 소속 상위 폴더 수정 날짜", example = "2023/12/05")
        @JsonFormat(pattern= "yy/MM/dd")
        private Timestamp editDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_10 : 노트이동 - 상위 폴더 목록 응답 DTO")
    public static class FolderParentListDTO {
        @Schema(description = "부모 폴더 ID")
        private Long parentFolderID;
        @Schema(description = "부모 폴더 이름")
        private String parentFolderName;
        @Schema(description = "부모 폴더 색상")
        private String parentFolderColor;
        @Schema(description = "폴더 즐겨찾기", example = "INACTIVE")
        private MarkStatus parentMarkState;
        @Schema(description = "폴더의 노트개수", example = "3")
        private Integer getNoteCount;
        @Schema(description = "폴더 목록")
        private List<FolderInfoDTO> foldersList;
        @Schema(description = "리스트 사이즈", example = "10")
        private Integer listSize;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_07 : 내부 요소 조회 응답 DTO")
    public static class getElementListDTO{
        @Schema(description = "조회 요청한 폴더 아이디", example = "1")
        private Long folderId;
        @Schema(description = "폴더 이름", example = "sample")
        private String name;
        @Schema(description = "폴더 색상", example = "ocean")
        private String color;
        @Schema(description = "즐겨찾기한 요소 리스트")
        private markElementList markElementList;
        @Schema(description = "즐겨찾기 하지 않은 요소 리스트")
        private notMarkElementList notMarkElementList;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_07_01 : 내부 요소(즐겨찾기 O) 조회 응답 DTO")
    public static class markElementList{
        List<FolderInfoDTO> folderList;
        List<NoteResponse.NoteInfoDTO> noteList;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_07 : 내부 요소(즐겨찾기 X) 조회 응답 DTO")
    public static class notMarkElementList{
        List<FolderInfoDTO> folderList;
        List<NoteResponse.NoteInfoDTO> noteList;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_RES_07 : 최근 즐겨찾기한 폴더 DTO")
    public static class RecentFolderDTO {
        @Schema(description = "현재 폴더 ID", example = "1")
        private Long folderId;
        @Schema(description = "현재 폴더 이름", example = "test")
        private String name;
        @Schema(description = "현재 폴더 색상", example = "ocean")
        private String color;
        @Schema(description = "즐겨찾기 상태", example = "ACTIVE")
        private MarkStatus markState;
        @Schema(description = "폴더 즐겨찾기 수정 날짜", example = "2023/12/05")
        private String markDate;
        @Schema(description = "폴더의 노트개수", example = "3")
        private Integer noteCount;
    }

}
