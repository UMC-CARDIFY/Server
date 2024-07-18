package com.umc.cardify.service;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;

    public Folder getFolder(long folderId){
        return folderRepository.getById(folderId);
    }
}
