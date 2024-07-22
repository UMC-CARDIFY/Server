package com.umc.cardify.service;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.repository.FolderRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    public Folder getFolder(long folderId){
        return folderRepository.getById(folderId);
    }

    public FolderResponse.FolderListDTO getFoldersByUserId(Long userId, int page, int size){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        Pageable pageable = PageRequest.of(page, size);
        Page<Folder> folderPage = folderRepository.findByUser(user, pageable);

        List<FolderResponse.FolderInfoDTO> folders = folderPage.getContent().stream()
                .map(folder -> FolderResponse.FolderInfoDTO.builder()
                        .folderId(folder.getFolderId())
                        .name(folder.getName())
                        .color(folder.getColor())
                        .markState(folder.getMarkState())
                        .getNoteCount(folder.getNoteCount())
                        .editDate(folder.getEditDate())
                        .createdAt(folder.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FolderResponse.FolderListDTO.builder()
                .foldersList(folders)
                .listSize(folderPage.getSize())
                .currentPage(folderPage.getNumber()+1)
                .totalPages(folderPage.getTotalPages())
                .totalElements(folderPage.getTotalElements())
                .isFirst(folderPage.isFirst())
                .isLast(folderPage.isLast())
                .build();
    }
}
