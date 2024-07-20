package com.umc.cardify.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class NoteResponse {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_01 : 노트 작성 응답 DTO")
    public static class WriteResultDTO{
        Long noteId;
        LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class NoteDTO {
        Long noteId;
        String name;
        String contents;
        LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class GetAllResultDTO {
        private List<NoteDTO> notes;
        private int currentPage;
        private int totalPages;
        private long totalElements;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_04 : 노트 공유 응답 DTO")
    public static class ShareResultDTO{
        String uuid;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_05 : 노트 UUID 검색 응답 DTO")
    public static class SearchUUIDResultDTO{
        Long noteId;
        String name;
        String contents;
        Boolean isEdit;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "NOTE_RES_06 : 노트 삭제 응답 DTO")
    public static class deleteNoteResultDTO{
        Boolean isSuccess;
    }
}
