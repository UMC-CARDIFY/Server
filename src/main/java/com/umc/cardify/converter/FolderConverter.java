package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.dto.folder.FolderResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class FolderConverter {

    public static FolderResponse.GetAllFolderResultDTO toGetAllResult(Page<Folder> folderPage) {
        List<FolderResponse.FolderDTO> folderDTOs = folderPage.getContent().stream()
                .map(folder -> FolderResponse.FolderDTO.builder()
                        .folderId(folder.getFolderId())
                        .name(folder.getName())
                        .createdAt(folder.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FolderResponse.GetAllFolderResultDTO.builder()
                .folders(folderDTOs)
                .currentPage(folderPage.getNumber())
                .totalPages(folderPage.getTotalPages())
                .totalElements(folderPage.getTotalElements())
                .build();
    }
}
