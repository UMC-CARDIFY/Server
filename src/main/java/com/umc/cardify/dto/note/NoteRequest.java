package com.umc.cardify.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.UUID;

public class NoteRequest {
    @Getter
    @Schema(title = "NOTE_REQ_01 : 노트 UUID 검색 요청 DTO")
    public static class SearchUUIDDto{
        @NotBlank
        @UUID
        String uuid;
    }
    @Getter
    @Schema(title = "NOTE_REQ_02 : 노트 검색 요청 DTO")
    public static class SearchNoteDto{
        @NotNull
        Long folderId;
        @NotNull
        String searchTxt;
    }
}
