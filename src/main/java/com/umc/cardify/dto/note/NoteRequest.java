package com.umc.cardify.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class NoteRequest {
    @Getter
    public static class WriteDto{
        @NotNull
        Integer folderId;
        @NotBlank
        String name;
        @NotBlank
        String contents;
    }

    @Getter
    public static class ShareDto{
        @NotNull
        Integer noteId;
        @NotNull
        Boolean isEdit;
    }
    @Getter
    public static class SearchUUIDDto{
        @NotBlank
        String uuid;
    }
}
