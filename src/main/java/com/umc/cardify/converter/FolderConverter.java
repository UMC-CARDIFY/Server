package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.dto.folder.FolderResponse;
import org.springframework.stereotype.Component;

@Component
public class FolderConverter {

    public FolderResponse.FolderInfoDTO toFolderInfoDTO(Folder folder) {
        return FolderResponse.FolderInfoDTO.builder()
                .folderId(folder.getFolderId())
                .name(folder.getName())
                .color(folder.getColor())
                .markState(folder.getMarkState())
                .getNoteCount(folder.getNoteCount())
                .editDate(folder.getEditDate())
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
