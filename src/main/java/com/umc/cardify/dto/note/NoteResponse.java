package com.umc.cardify.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "노트 응답 데이터")
public class NoteResponse {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WriteResultDTO{
        Long noteId;
        LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareResultDTO{
        String uuid;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchUUIDResultDTO{
        Long noteId;
        String name;
        String contents;
        Boolean isEdit;
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
        @Schema(description = "노트 수정일", example = "2023-07-18T01:40:13")
        Timestamp editDate;
        @Schema(description = "노트 생성 날짜", example = "2023-07-10T12:34:56")
        LocalDateTime createdAt;
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
}
