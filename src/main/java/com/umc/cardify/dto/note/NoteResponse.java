package com.umc.cardify.dto.note;

import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.enums.MarkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class NoteResponse {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_01 : 노트 추가 응답 DTO")
    public static class AddNoteResultDTO{
        Long noteId;
        LocalDateTime createdAt;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_04 : 노트 정보 DTO")
    public static class NoteInfoDTO {
        @Schema(description = "노트 아이디", example = "1")
        private Long noteId;
        @Schema(description = "노트 이름", example = "Sample Note")
        private String name;
        @Schema(description = "폴더 이름", example = "Sample Folder")
        private Long folderId;
        @Schema(description = "폴더 이름", example = "Sample Folder")
        private String folderName;
        @Schema(description = "폴더 색상", example = "blue")
        private String folderColor;
        @Schema(description = "노트 즐겨찾기", example = "ACTIVE")
        private MarkStatus markState;
        @Schema(description = "플래시카드 개수", example = "3")
        private Long flashCardCount;
        @Schema(description = "노트 열람일", example = "2023-07-18")
        private LocalDateTime viewAt;
        @Schema(description = "노트 즐겨찾기 날짜", example = "yy/MM/dd")
        private String markAt;
        @Schema(description = "노트 수정일", example = "2023-07-18")
        private String editDate;
        @Schema(description = "노트 생성 날짜", example = "2023-07-10")
        private String createdAt;
        @Schema(description = "다운로드한 노트 여부", example = "true")
        private Boolean isDownload;
        @Schema(description = "업로드한 노트 여부", example = "true")
        private Boolean isUpload;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_05 : 노트 목록 응답 DTO")
    public static class NoteListDTO {
        @Schema(description = "노트 목록")
        private List<NoteInfoDTO> noteList;
        @Schema(description = "리스트 사이즈", example = "10")
        private Integer listsize;
        @Schema(description = "현재 페이지 번호", example = "1")
        private Integer currentPage;
        @Schema(description = "총 페이지 수", example = "5")
        private Integer totalPage;
        @Schema(description = "총 노트 수", example = "10")
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
    @Schema(title = "NOTE_RES_06 : API 실행 성공 여부 응답 DTO")
    public static class IsSuccessNoteDTO{
        Boolean isSuccess;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_07 : 특정 폴더 내 노트 조회 응답 DTO")
    public static class GetNoteToFolderResultDTO{
        String folderName;
        String folderColor;
        @Schema(description = "노트 목록")
        List<NoteInfoDTO> noteList;
        @Schema(description = "리스트 사이즈", example = "10")
        Integer listSize;
        @Schema(description = "현재 페이지 번호", example = "1")
        Integer currentPage;
        @Schema(description = "총 페이지 수", example = "5")
        Integer totalPage;
        @Schema(description = "총 노트 수", example = "10")
        Long totalElements;
        @Schema(description = "첫 페이지인지 확인", example = "true")
        Boolean isFirst;
        @Schema(description = "마지막 페이지인지 확인", example = "false")
        Boolean isLast;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_08_0 : 노트 검색 응답 DTO")
    public static class SearchNoteResDTO{
        Long noteId;
        String noteName;
        List<String> textList;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_08 : 폴더 노트 검색 응답 DTO")
    public static class SearchNoteDTO{
        String searchTxt;
        List<SearchNoteResDTO> noteList;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_08 : 전체 노트 검색 응답 DTO")
    public static class SearchNoteAllDTO{
        String searchTxt;
        List<SearchNoteToUserDTO> noteToUserList;
        List<SearchNoteToLibDTO> noteToLibList;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_08_1 : 전체 노트 검색 응답 DTO(유저)")
    public static class SearchNoteToUserDTO{
        Long folderId;
        String folderName;
        Long parentsFolderId;
        String parentsFolderName;
        List<SearchNoteResDTO> noteList;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_08_2 : 전체 노트 검색 응답 DTO(자료실)")
    public static class SearchNoteToLibDTO{
        Long libraryId;
        SearchNoteResDTO note;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_09-1 : 노트 조회 응답(카드) DTO")
    public static class getNoteCardDTO{
        Long cardId;
        String cardName;
        String contentsFront;
        String contentsBack;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_09 : 노트 조회 응답 DTO")
    public static class getNoteDTO{
        Long noteId;
        String noteName;
        Object noteContent;
        Boolean markState;
        Boolean isEdit;
        Boolean isUpload;
        List<getNoteCardDTO> cardList;
    }
}
