package com.umc.cardify.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.UUID;

import java.util.List;

public class NoteRequest {
    @Getter
    @Schema(title = "NOTE_REQ_01 : 노트 UUID 검색 요청 DTO")
    public static class SearchUUIDDto{
        @NotBlank
        @UUID
        String uuid;
    }
    @Getter
    @Schema(title = "NOTE_REQ_02_01 : 노트 작성시 카드 양식 DTO")
    public static class WriteCardDto{
        @NotNull
        String name;
        @NotNull
        String text;
    }
    @Getter
    @Schema(title = "NOTE_REQ_02 : 노트 작성 요청 DTO")
    public static class WriteNoteDto{
        @NotNull
        Long noteId;
        @NotNull
        String name;
        @NotNull
        String contents;
        List<WriteCardDto> cards;
    }
}
