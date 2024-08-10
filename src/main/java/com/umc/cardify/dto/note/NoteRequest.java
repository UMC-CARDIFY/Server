package com.umc.cardify.dto.note;

import com.umc.cardify.dto.card.CardRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.UUID;

import java.util.List;

public class NoteRequest {
    @Getter
    @Schema(title = "NOTE_REQ_02 : 노트 작성 요청 DTO")
    public static class WriteNoteDto{
        @NotNull
        Long noteId;
        @NotNull
        String name;
        @NotNull
        String contents;
    }
    @Getter
    @Schema(title = "NOTE_REQ_03 : 특정 폴더 내 노트 조회 요청 DTO")
    public static class GetNoteToFolderDto{
        @NotNull
        Long folderId;
        @Schema(description = "페이지 번호", example = "0")
        Integer page;
        @Schema(description = "한 페이지 당 사이즈", example = "5")
        Integer size;
        @Schema(description = "정렬 방식", example = "asc")
        String order;
    }
    @Getter
    @Schema(title = "NOTE_REQ_04 : 노트 검색 요청 DTO")
    public static class SearchNoteDto{
        @NotNull
        Long folderId;

        String searchTxt;
    }
    @Getter
    @Schema(title = "NOTE_REQ_06 : 노트 자료실 업로드 요청 DTO")
    public static class ShareLibDto{
        @NotNull
        Long noteId;
        List<String> category;
    }
}
