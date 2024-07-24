package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.repository.FolderRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public Folder getFolder(long folderId){
        return folderRepository.findById(folderId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
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

    public FolderResponse.sortFolderListDTO sortFoldersByUserId(Long userId, int page, int size, String order){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found with id: " + userId));
        Pageable pageable = switch (order.toLowerCase()) {
            case "asc" -> PageRequest.of(page, size, Sort.by("name").ascending());
            case "desc" -> PageRequest.of(page, size, Sort.by("name").descending());
            case "edit-newest" -> PageRequest.of(page, size, Sort.by("editDate").ascending());
            case "edit-oldest" -> PageRequest.of(page, size, Sort.by("editDate").descending());
            default -> throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        };

        Page<Folder> folderPage = folderRepository.findByUser(user, pageable);
        List<FolderResponse.sortFolderInfoDTO> folders = folderPage.getContent().stream()
                .map(folder -> FolderResponse.sortFolderInfoDTO.builder()
                        .folderId(folder.getFolderId())
                        .name(folder.getName())
                        .editDate(folder.getEditDate())
                        .createdAt(folder.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FolderResponse.sortFolderListDTO.builder()
                .sortFoldersList(folders)
                .listSize(folderPage.getSize())
                .currentPage(folderPage.getNumber()+1)
                .totalPages(folderPage.getTotalPages())
                .totalElements(folderPage.getTotalElements())
                .isFirst(folderPage.isFirst())
                .isLast(folderPage.isLast())
                .build();
    }

    @Transactional
    public void deleteFolderById(Long userId, Long folderId) {
        User user = userRepository.findById(userId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.REQUEST_ERROR));
        Folder folder = folderRepository.findByFolderIdAndUser(folderId, user).orElseThrow(() -> new BadRequestException(ErrorResponseStatus.REQUEST_ERROR));

        noteRepository.deleteByFolder(folder);
        folderRepository.delete(folder);
    }

    @Transactional
    public FolderResponse.addFolderResultDTO addFolder(Long userId, FolderRequest.addFolderDto folderRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (folderRepository.existsByUserAndName(user, folderRequest.getName())) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_ERROR);
        }

        Folder folder = Folder.builder()
                .name(folderRequest.getName())
                .color(folderRequest.getColor())
                .user(user)
                .build();

        folder = folderRepository.save(folder);

        return FolderResponse.addFolderResultDTO.builder()
                .folderId(folder.getFolderId())
                .name(folder.getName())
                .color(folder.getColor())
                .createdAt(folder.getCreatedAt())
                .build();
    }

    @Transactional
    public FolderResponse.editFolderResultDTO editFolder(Long userId, Long folderId, FolderRequest.editFolderDto folderRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Folder folder = folderRepository.findByFolderIdAndUser(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found with id: " + folderId));

        if (folderRepository.existsByUserAndName(user, folderRequest.getName())) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_ERROR);
        }

        folder.setName(folderRequest.getName());
        folder.setColor(folderRequest.getColor());

        folder = folderRepository.save(folder);

        return FolderResponse.editFolderResultDTO.builder()
                .folderId(folder.getFolderId())
                .name(folder.getName())
                .color(folder.getColor())
                .editDate(folder.getEditDate())
                .build();
    }
}
