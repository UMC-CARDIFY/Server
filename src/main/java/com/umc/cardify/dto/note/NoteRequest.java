package com.umc.cardify.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class NoteRequest {
    @Getter
    public static class writeDto{
        @NotNull
        Integer folderId;
        @NotBlank
        String name;
        @NotBlank
        String contents;
    }
    @Getter
    public static class shareDto{
        @NotNull
        Integer noteId;
        @NotNull
        Boolean isEdit;
    }
}
