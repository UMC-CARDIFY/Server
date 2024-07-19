package com.umc.cardify.dto.note;

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
    public static class getAllDto{
        @NotNull
        Integer folderId;
        @NotNull
        Integer noteId;
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
