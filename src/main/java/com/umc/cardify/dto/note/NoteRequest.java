package com.umc.cardify.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.UUID;

public class NoteRequest {
    @Getter
    @Schema(title = "NOTE_REQ_01 : 노트 작성 요청 DTO")
    public static class WriteDto{
        @NotNull
        Integer folderId;
        @NotBlank
        String name;
        @NotBlank
        String contents;
    }

    @Getter
    @Schema(title = "NOTE_REQ_02 : 노트 공유 요청 DTO")
    public static class ShareDto{
        @NotNull
        Integer noteId;
        @NotNull
        Boolean isEdit;
    }
    @Getter
    @Schema(title = "NOTE_REQ_03 : 노트 UUID 검색 요청 DTO")
    public static class SearchUUIDDto{
        @NotBlank
        @UUID
        String uuid;
    }
    @Getter
    @Schema(title = "NOTE_REQ_04 : 노트 삭제 요청 DTO")
    public static class DeleteNoteDto{
        @NotNull
        Long noteId;
    }
}
