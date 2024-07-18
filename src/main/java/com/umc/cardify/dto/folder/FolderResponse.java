package com.umc.cardify.dto.folder;

import com.umc.cardify.domain.enums.MarkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class FolderResponse {
    @Getter
    @Builder
    public static class FolderDTO {
        Integer folderId;
        String name;
        String color;
        LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class GetAllFolderResultDTO {
        List<FolderDTO> folders;
        int currentPage;
        int totalPages;
        long totalElements;
    }
}
