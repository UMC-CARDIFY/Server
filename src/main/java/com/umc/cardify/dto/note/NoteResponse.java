package com.umc.cardify.dto.note;

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
        Long noteId;
        @Schema(description = "노트 이름", example = "Sample Note")
        String name;
        @Schema(description = "폴더 이름", example = "Sample Folder")
        String folderName;
        @Schema(description = "노트 즐겨찾기", example = "ACTIVE")
        MarkStatus markState;
        @Schema(description = "노트 수정일", example = "2023-07-18")
        String editDate;
        @Schema(description = "노트 생성 날짜", example = "2023-07-10")
        String createdAt;
        @Schema(description = "다운로드한 노트 여부", example = "true")
        Boolean isDownload;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_05 : 노트 목록 응답 DTO")
    public static class NoteListDTO {
        @Schema(description = "노트 목록")
        List<NoteInfoDTO> noteList;
        @Schema(description = "리스트 사이즈", example = "10")
        Integer listsize;
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
    @Schema(title = "NOTE_RES_08_01 : 노트 검색 응답 DTO")
    public static class SearchNoteResDTO{
        Long noteId;
        String noteName;
        List<String> textList;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_08 : 노트 검색 응답 DTO")
    public static class SearchNoteDTO{
        String searchTxt;
        List<SearchNoteResDTO> noteList;
    }
}
