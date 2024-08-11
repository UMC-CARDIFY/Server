package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.folder.FolderComparator;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.repository.FolderRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.jdbc.Null;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
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

    @Transactional(readOnly = true)
    public FolderResponse.FolderListDTO getFoldersByUserId(Long userId, Integer page, Integer size){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 폴더 page, size에 값을 입력하지 않으면, 자동으로 0과 30으로 고정
        int getFolderPage = (page!=null) ? page:0;
        int getFolderSize = (size!=null) ? size:30;

        Pageable pageable = PageRequest.of(getFolderPage, getFolderSize);
        Page<Folder> folderPage = folderRepository.findByUser(user, pageable);

        if(folderPage.isEmpty()){
            throw new BadRequestException(ErrorResponseStatus.NOT_EXIST_FOLDER);
        }

        List<FolderResponse.FolderInfoDTO> folders = folderPage.getContent().stream()
                .map(folder -> FolderResponse.FolderInfoDTO.builder()
                        .folderId(folder.getFolderId())
                        .name(folder.getName())
                        .color(folder.getColor())
                        .markState(folder.getMarkState())
                        .getNoteCount(folder.getNoteCount())
                        .markDate(folder.getMarkDate())
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

    public FolderResponse.sortFolderListDTO sortFoldersByUserId(Long userId, Integer page, Integer size, String order){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        int sortFolderPage = (page != null) ? page : 0;
        int sortFolderSize = (size != null) ? size : 30;

        Pageable pageable = PageRequest.of(sortFolderPage, sortFolderSize);
        Page<Folder> folderPage = folderRepository.findByUserAndSort(user, order, pageable);

        switch (order) {
            case "asc":
            case "desc":
            case "edit-newest":
            case "edit-oldest":
                break;
            default:
                throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }

        List<FolderResponse.sortFolderInfoDTO> folders = folderPage.getContent().stream()
                .sorted(new FolderComparator())
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
                .currentPage(folderPage.getNumber() + 1)
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
                .orElseThrow(()-> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        if (folderRepository.existsByUserAndName(user, folderRequest.getName())) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_ERROR);
        } else {
            Folder folder = Folder.builder()
                    .name(folderRequest.getName())
                    .color(folderRequest.getColor())
                    .user(user)
                    .markState(MarkStatus.INACTIVE)
                    .build();

            folder = folderRepository.save(folder);

            return FolderResponse.addFolderResultDTO.builder()
                    .folderId(folder.getFolderId())
                    .name(folder.getName())
                    .color(folder.getColor())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    @Transactional
    public FolderResponse.editFolderResultDTO editFolder(Long userId, Long folderId, FolderRequest.editFolderDto folderRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        Folder folder = folderRepository.findByFolderIdAndUser(folderId, user)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_EXIST_FOLDER));

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

    @Transactional
    public FolderResponse.markFolderResultDTO markFolderById(Long userId, Long folderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BadRequestException(ErrorResponseStatus.REQUEST_ERROR));

        Folder folder = folderRepository.findByFolderIdAndUser(folderId, user)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.REQUEST_ERROR));

        if(folder.getMarkState() == MarkStatus.ACTIVE){
            folder.setMarkState(MarkStatus.INACTIVE);
        } else {
            folder.setMarkState(MarkStatus.ACTIVE);
        }

        folder.setMarkDate(Timestamp.valueOf(LocalDateTime.now()));
        folderRepository.save(folder);

        return FolderResponse.markFolderResultDTO.builder()
                .markState(folder.getMarkState())
                .isSuccess(true)
                .markDate(folder.getMarkDate())
                .build();
    }

    @Transactional(readOnly = true)
    public FolderResponse.FolderListDTO filterColorsByUserId(Long userId, Integer page, Integer size, String colors) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        int filterPage = (page != null) ? page : 0;
        int filterSize = (size != null) ? size : 30;

        if (colors == null || colors.isEmpty()) {
            throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_FOLDER);
        }

        Pageable pageable = PageRequest.of(filterPage, filterSize);
        Page<Folder> folderPage = folderRepository.findByUserAndColor(user, colors, pageable);

        if(folderPage.isEmpty()){
            throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_FOLDER);
        }

        List<FolderResponse.FolderInfoDTO> folders = folderPage.stream()
                .map(folder -> FolderResponse.FolderInfoDTO.builder()
                        .folderId(folder.getFolderId())
                        .name(folder.getName())
                        .color(folder.getColor())
                        .markState(folder.getMarkState())
                        .markDate(folder.getMarkDate())
                        .createdAt(folder.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FolderResponse.FolderListDTO.builder()
                .foldersList(folders)
                .listSize(filterSize)
                .currentPage(filterPage + 1)
                .totalPages(folderPage.getTotalPages())
                .totalElements(folderPage.getTotalElements())
                .isFirst(folderPage.isFirst())
                .isLast(folderPage.isLast())
                .build();
    }
}
