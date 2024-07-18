package com.umc.cardify.service;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.repository.FolderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;

    public Folder getFolder(long folderId){
        return folderRepository.getById(folderId);
    }

//    public Folder getFolder(long folderId){
//        return folderRepository.findById(folderId)
//                .orElseThrow(() -> new EntityNotFoundException("폴더가 존재하지 않습니다.: " + folderId));
//    }

    public Page<Folder> getAllFolders(Long folderId, Pageable pageable) {
        if (folderId != null) {
            return folderRepository.findAllByFolderId(folderId, pageable);
        }
        return folderRepository.findAll(pageable);
    }

}
