package com.umc.cardify.dto.library;

import com.umc.cardify.dto.card.CardRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

public class LibraryRequest {
    @Getter
    @Schema(title = "LIBRARY_REQ_01 : 자료실 다운로드 요청 DTO")
    public static class DownloadLibDto{
        @NotNull
        Long libraryId;
        @NotNull
        Long folderId;
        @NotNull
        Boolean isContainCard;
    }
    @Getter
    @Schema(title = "LIBRARY_REQ_02 : 자료실 검색 요청 DTO")
    public static class SearchLibDto{
        String searchTxt;
        List<String> categoryList;
    }
}
