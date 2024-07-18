package com.umc.cardify.dto.note;

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
}
