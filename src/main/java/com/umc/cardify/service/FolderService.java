package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.folder.FolderComparator;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.repository.FolderRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public Folder getFolder(long folderId){
        return folderRepository.findById(folderId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
    }

    @Transactional
    public FolderResponse.FolderListDTO getFoldersByUserId(Long userId, Integer page, Integer size){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 폴더 page, size에 값을 입력하지 않으면, 자동으로 0과 최대값으로 고정
        int getFolderPage = (page!=null) ? page:0;
        int getFolderSize = (size!=null) ? size:Integer.MAX_VALUE;

        Pageable pageable = PageRequest.of(getFolderPage, getFolderSize);
        Page<Folder> folderPage = folderRepository.findByUser(user, pageable);

        if(folderPage.isEmpty()){
            throw new BadRequestException(ErrorResponseStatus.NOT_EXIST_FOLDER);
        }


        List<FolderResponse.FolderInfoDTO> folders = folderPage.getContent().stream()
                .map(folder -> {
                    Note latestNote = noteRepository.findTopByFolderOrderByEditDateDesc(folder);
                    Timestamp latestNoteEditDate = latestNote != null ? latestNote.getEditDate() : null;

                    if (latestNoteEditDate != null) {
                        if (folder.getEditDate() == null || latestNoteEditDate.after(folder.getEditDate())) {
                            folder.setEditDate(latestNoteEditDate);
                            folderRepository.save(folder); //비교해서 가장 최신 수정일로 저장함
                        }
                    }

                    return FolderResponse.FolderInfoDTO.builder()
                            .folderId(folder.getFolderId())
                            .name(folder.getName())
                            .color(folder.getColor())
                            .markState(folder.getMarkState())
                            .getNoteCount(folder.getNoteCount())
                            .markDate(folder.getMarkDate())
                            .editDate(folder.getEditDate())
                            .createdAt(folder.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return FolderResponse.FolderListDTO.builder()
                .foldersList(folders)
                .listSize(folderPage.getSize())
                .currentPage(folderPage.getNumber()+1)
                .totalPages(folderPage.getTotalPages())
                .totalElements(folderPage.getNumberOfElements())
                .isFirst(folderPage.isFirst())
                .isLast(folderPage.isLast())
                .build();
    }

    @Transactional
    public FolderResponse.FolderListDTO getFoldersBySortFilter(Long userId, Integer page, Integer size, String order, String color){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        int folderPage = (page != null) ? page : 0;
        int folderSize = (size != null) ? size : Integer.MAX_VALUE;

        List<Folder> filteredFolders = filterFoldersByColor(user, color);

        List<Folder> sortedFolders = sortFolders(filteredFolders, order);

        List<Folder> pagedFolders = paginateFolders(sortedFolders, folderPage, folderSize);

        List<FolderResponse.FolderInfoDTO> folderInfoDTOS = convertToFolderInfoDTOs(pagedFolders);

        int totalElements = sortedFolders.size();
        int totalPages = (totalElements + folderSize - 1) / folderSize;

        return FolderResponse.FolderListDTO.builder()
                .foldersList(folderInfoDTOS)
                .listSize(pagedFolders.size())
                .currentPage(folderPage + 1)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .isFirst(folderPage == 0)
                .isLast(folderPage == totalPages - 1)
                .build();
    }
    private List<Folder> filterFoldersByColor(User user, String colors) {
        if (colors == null || colors.isEmpty()) {
            return folderRepository.findByUser(user);
        }

        List<String> colorList = Arrays.asList(colors.split(","));
        return folderRepository.findByUserAndColor(user, colorList);
    }

    private List<Folder> sortFolders(List<Folder> folders, String order) {
        if (order == null || order.isEmpty()) {
            return folders;
        }

        return folders.stream()
                .sorted(new FolderComparator(order)
                        .thenComparing(Folder::getFolderId))
                .collect(Collectors.toList());
    }

    private List<Folder> paginateFolders(List<Folder> folders, int page, int size) {
        int start = page * size;
        int end = Math.min((page + 1) * size, folders.size());
        return folders.subList(start, end);
    }

    private List<FolderResponse.FolderInfoDTO> convertToFolderInfoDTOs(List<Folder> folders) {
        return folders.stream()
                .map(folder -> {
                    Note latestNote = noteRepository.findTopByFolderOrderByEditDateDesc(folder);
                    Timestamp latestNoteEditDate = latestNote != null ? latestNote.getEditDate() : null;

                    if (latestNoteEditDate != null) {
                        if (folder.getEditDate() == null || latestNoteEditDate.after(folder.getEditDate())) {
                            folder.setEditDate(latestNoteEditDate);
                            folderRepository.save(folder);
                        }
                    }

                    return FolderResponse.FolderInfoDTO.builder()
                            .folderId(folder.getFolderId())
                            .name(folder.getName())
                            .color(folder.getColor())
                            .markState(folder.getMarkState())
                            .getNoteCount(folder.getNoteCount())
                            .markDate(folder.getMarkDate())
                            .editDate(folder.getEditDate())
                            .createdAt(folder.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
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

        if (folderRepository.existsByUserAndName(user, folderRequest.getName()) && !folderId.equals(folder.getFolderId())) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_ERROR);
        }

        folder.setEditDate(Timestamp.valueOf(LocalDateTime.now())); //수정된 시간을 저장
        folder.setName(folderRequest.getName()); // 수정된 이름 저장
        folder.setColor(folderRequest.getColor()); // 수정된 색상 저장

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
}
