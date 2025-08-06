package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.domain.enums.SubscriptionStatus;
import com.umc.cardify.dto.folder.FolderComparator;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.repository.FolderRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
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

    // 상위/하위 폴더 조회, 정렬, 필터링
    @Transactional
    public FolderResponse.FolderListDTO getFoldersBySortFilter(Long userId, Long parentFolderId, Integer page, Integer size, String order, String color) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        int folderPage = (page != null) ? page : 0;
        int folderSize = (size != null) ? size : Integer.MAX_VALUE;

        List<Folder> folders;
        String parentFolderName = "";
        String parentFolderColor = "";
        MarkStatus parnetMarkStatus = MarkStatus.ACTIVE;

        // 1. 상위 폴더와 하위 폴더를 구분하여 기본 폴더 리스트 가져오기
        if (parentFolderId == null) {
            // 상위 폴더 조회
            folders = folderRepository.findByUserAndParentFolderIsNull(user);
        } else {
            // 하위 폴더 조회
            Folder parentFolder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_EXIST_FOLDER));
            folders = folderRepository.findByParentFolderAndUser(parentFolder, user);
            parentFolderName = parentFolder.getName();
            parentFolderColor = parentFolder.getColor();
            parnetMarkStatus = parentFolder.getMarkState();
        }

        // 2. 색상 필터 적용
        List<Folder> filteredFolders = filterFoldersByColor(folders, color);

        // 3. 정렬 적용
        List<Folder> sortedFolders = sortFolders(filteredFolders, order);

        // 4. 페이징 적용
        List<Folder> pagedFolders = pagingFolders(sortedFolders, folderPage, folderSize);

        // 5. FolderListDTO 생성
        List<FolderResponse.FolderInfoDTO> foldersInfo = convertToFolderInfoDTOs(pagedFolders);
        int totalElements = sortedFolders.size();
        int totalPages = (totalElements + folderSize - 1) / folderSize;

        return FolderResponse.FolderListDTO.builder()
                .parentFolderColor(parentFolderColor)
                .parentFolderName(parentFolderName)
                .parentMarkState(parnetMarkStatus)
                .foldersList(foldersInfo)
                .listSize(pagedFolders.size())
                .currentPage(folderPage + 1)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .isFirst(folderPage == 0)
                .isLast(folderPage == totalPages - 1)
                .build();
    }

    private List<Folder> filterFoldersByColor(List<Folder> folders, String colors) {
        if (colors == null || colors.isEmpty()) {
            return folders;
        }

        List<String> colorList = Arrays.asList(colors.split(","));
        List<String> allowedColors = Arrays.asList("blue", "ocean", "lavender", "mint", "sage", "gray", "orange", "coral", "rose", "plum");

        colorList.forEach(color -> {
            if (!allowedColors.contains(color)) {
                throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
            }
        });

        return folders.stream()
                .filter(folder -> colorList.contains(folder.getColor()))
                .collect(Collectors.toList());
    }

    private List<Folder> sortFolders(List<Folder> folders, String order) {
        if (order == null || order.isEmpty()) {
            return folders;
        }

        List<String> orderList = Arrays.asList("asc", "desc", "edit-newest", "edit-oldest");
        if (!orderList.contains(order)) {
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }

        return folders.stream()
                .sorted(new FolderComparator(order).thenComparing(Folder::getFolderId))
                .collect(Collectors.toList());
    }

    private List<Folder> pagingFolders(List<Folder> folders, int page, int size) {
        int start = page * size;
        int end = Math.min((page + 1) * size, folders.size());
        return folders.subList(start, end);
    }

    private List<FolderResponse.FolderInfoDTO> convertToFolderInfoDTOs(List<Folder> folders) {
        return folders.stream()
                .map(folder -> {
                    Note latestNote = noteRepository.findTopByFolderOrderByEditDateDesc(folder);
                    Timestamp latestNoteEditDate = latestNote != null ? latestNote.getEditDate() : null;

                    if (latestNoteEditDate != null && (folder.getEditDate() == null || latestNoteEditDate.after(folder.getEditDate()))) {
                        folder.setEditDate(latestNoteEditDate);
                        folderRepository.save(folder);
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

    //상위 폴더 생성
    @Transactional
    public FolderResponse.addFolderResultDTO addFolder(Long userId, FolderRequest.addFolderDto folderRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        boolean isSubscribed = user.getSubscriptions().stream()
                .anyMatch(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE);

        // 유료 결제 여부에 따른 상위 폴더 개수 제한
        int folderCount = folderRepository.countByUserAndParentFolderIsNull(user);
        if (!isSubscribed && folderCount >= 9) { // 무료 사용자 제한
            throw new BadRequestException(ErrorResponseStatus.FOLDER_CREATED_NOT_ALLOWED);
        }

        // 중복 이름 폴더 확인
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

    //하위 폴더 생성
    @Transactional
    public FolderResponse.addFolderResultDTO addSubFolder(Long userId, FolderRequest.addSubFolderDto subFolderRequest, Long folderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        Folder parentFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_EXIST_FOLDER));

        // 상위 폴더가 이미 하위 폴더라면 생성 제한 || 하위 폴더 개수 제한
        int subFolderCount = folderRepository.countByParentFolder(parentFolder);
        if (parentFolder.getParentFolder() != null || subFolderCount >= 100) {
            throw new BadRequestException(ErrorResponseStatus.SUBFOLDER_CREATION_NOT_ALLOWED);
        }

        boolean isSubscribed = user.getSubscriptions().stream()
                .anyMatch(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE);

        // 유료 결제 여부에 따른 하위 폴더 개수 제한
        if (!isSubscribed && subFolderCount >= 9) { // 무료 사용자 제한
            throw new BadRequestException(ErrorResponseStatus.SUBFOLDER_CREATION_NOT_ALLOWED);
        }

        Folder newSubFolder = Folder.builder()
                .user(user)
                .name(subFolderRequest.getName())
                .color(parentFolder.getColor())
                .markState(MarkStatus.INACTIVE)
                .parentFolder(parentFolder)
                .build();

        newSubFolder = folderRepository.save(newSubFolder);
        return FolderResponse.addFolderResultDTO.builder()
                .parent_folderId(newSubFolder.getParentFolder().getFolderId())
                .folderId(newSubFolder.getFolderId())
                .name(newSubFolder.getName())
                .color(newSubFolder.getColor())
                .createdAt(LocalDateTime.now())
                .build();
    }


    // 폴더 수정
    @Transactional
    public FolderResponse.editFolderResultDTO editFolder(Long userId, Long folderId, FolderRequest.editFolderDto folderRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        Folder folder = folderRepository.findByFolderIdAndUser(folderId, user)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_EXIST_FOLDER));

        if (folderRepository.existsByUserAndName(user, folderRequest.getName()) && !folderId.equals(folder.getFolderId())) {
            throw new BadRequestException(ErrorResponseStatus.DUPLICATE_ERROR);
        }

        boolean isParentFolder = folder.getParentFolder() == null;

        // 자식 폴더일때 색상 변경하면 에러처리
        if (!isParentFolder && folderRequest.getColor() != null && !folderRequest.getColor().equals(folder.getColor())) {
            throw new BadRequestException(ErrorResponseStatus.SUB_FOLDER_COLOR_CHANGE_NOT_ALLOWED);
        }

        folder.setName(folderRequest.getName()); // 수정된 이름 저장

        // 부모 폴더인 경우에만 색상 변경 허용 및 하위 폴더에 반영
        if (isParentFolder && folderRequest.getColor() != null && !folderRequest.getColor().equals(folder.getColor())) {
            folder.setColor(folderRequest.getColor());
            List<Folder> subFolders = folderRepository.findAllByParentFolder(folder);
            for (Folder sub : subFolders) {
                sub.setColor(folderRequest.getColor());
            }
        }

        folder.setEditDate(Timestamp.valueOf(LocalDateTime.now())); //수정된 시간을 저장
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

    @Transactional(readOnly = true)
    public FolderResponse.ParentFolderListDTO searchParentFolders(Long userId, String keyword, Integer page, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 페이징 설정
        int getPage = (page != null) ? page : 0;
        int getSize = (size != null) ? size : 20;
        Pageable pageable = PageRequest.of(getPage, getSize);

        // 상위폴더 검색 (parentFolderId가 null인 폴더들)
        Page<Folder> parentFolders;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색어가 있는 경우
            parentFolders = folderRepository.findParentFoldersByKeyword(user, keyword.trim(), pageable);
        } else {
            // 검색어가 없는 경우 전체 상위폴더 조회
            parentFolders = folderRepository.findAllParentFolders(user, pageable);
        }

        // 각 상위폴더의 하위폴더 개수 조회
        List<FolderResponse.ParentFolderInfoDTO> parentFolderInfos = parentFolders.getContent()
                .stream()
                .map(folder -> {
                    Long subFolderCount = folderRepository.countSubFolders(folder);
                    return FolderResponse.ParentFolderInfoDTO.builder()
                            .folderId(folder.getFolderId())
                            .folderName(folder.getName())
                            .folderColor(folder.getColor())
                            .markState(folder.getMarkState())
                            .getSubFolderCount(folder.getSubFolders().size())
                            .getNoteCount(folder.getNoteCount())
                            .createdAt(folder.getCreatedAt())
                            .editDate(folder.getEditDate())
                            .build();
                })
                .collect(Collectors.toList());

        return FolderResponse.ParentFolderListDTO.builder()
                .parentFolders(parentFolderInfos)
                .listSize(parentFolders.getSize())
                .currentPage(getPage + 1)
                .totalPages(parentFolders.getTotalPages())
                .totalElements(parentFolders.getTotalElements())
                .isFirst(parentFolders.isFirst())
                .isLast(parentFolders.isLast())
                .build();
    }

    public FolderResponse.FolderMoveResultDTO moveSubFolder(Long userId, Long subFolderId, Long targetParentFolderId) {
        log.info("폴더 이동 시작: userId={}, subFolderId={}, targetParentFolderId={}",
                userId, subFolderId, targetParentFolderId);

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 이동할 하위폴더 검증
        Folder subFolder = folderRepository.findById(subFolderId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));

        // 하위폴더인지 확인
        if (subFolder.getParentFolder() == null) {
            throw new BadRequestException(ErrorResponseStatus.CANNOT_MOVE_PARENT_FOLDER);
        }

        // 대상 상위폴더 검증
        Folder targetParentFolder = folderRepository.findById(targetParentFolderId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));

        // 대상이 상위폴더인지 확인
        if (targetParentFolder.getParentFolder() != null) {
            throw new BadRequestException(ErrorResponseStatus.TARGET_MUST_BE_PARENT_FOLDER);
        }

        // 자기 자신으로 이동하려는 경우 방지
        if (subFolder.getParentFolder().getFolderId().equals(targetParentFolderId)) {
            throw new BadRequestException(ErrorResponseStatus.ALREADY_IN_TARGET_FOLDER);
        }

        // 기존 상위폴더 정보 저장
        String previousParentName = subFolder.getParentFolder().getName();

        // 폴더 이동 실행
        subFolder.updateParentFolder(targetParentFolder);
        Folder savedFolder = folderRepository.save(subFolder);

        log.info("폴더 이동 완료: {} -> {}", previousParentName, targetParentFolder.getName());

        return FolderResponse.FolderMoveResultDTO.builder()
                .folderId(savedFolder.getFolderId())
                .folderName(savedFolder.getName())
                .previousParentFolderId(subFolder.getParentFolder().getFolderId())
                .previousParentFolderName(previousParentName)
                .newParentFolderId(targetParentFolder.getFolderId())
                .newParentFolderName(targetParentFolder.getName())
                .build();
    }

//    @Transactional(readOnly = true)
//    public FolderResponse.MovabilityCheckDTO checkMovability(Long userId, Long folderId) {
//        log.info("폴더 이동 가능성 확인: userId={}, folderId={}", userId, folderId);
//
//        // 사용자 검증
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
//
//        // 폴더 검증
//        Folder folder = folderRepository.findById(folderId)
//                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));
//
//        // 폴더 소유권 확인
//        if (!folder.getUser().getUserId().equals(userId)) {
//            return FolderResponse.MovabilityCheckDTO.builder()
//                    .movable(false)
//                    .reason("폴더 소유권이 없습니다.")
//                    .build();
//        }
//
//        // 상위폴더인 경우 이동 불가
//        if (folder.getParentFolder() == null) {
//            return FolderResponse.MovabilityCheckDTO.builder()
//                    .movable(false)
//                    .reason("상위폴더는 이동할 수 없습니다.")
//                    .build();
//        }
//
//        // 이동 가능한 상위폴더 개수 확인
//        Long availableParentFolders = folderRepository.countAvailableParentFolders(user, folder.getParentFolder().getFolderId());
//
//        if (availableParentFolders == 0) {
//            return FolderResponse.MovabilityCheckDTO.builder()
//                    .movable(false)
//                    .reason("이동할 수 있는 다른 상위폴더가 없습니다.")
//                    .build();
//        }
//
//        return FolderResponse.MovabilityCheckDTO.builder()
//                .movable(true)
//                .availableTargetCount(availableParentFolders)
//                .currentParentFolderId(folder.getParentFolder().getFolderId())
//                .currentParentFolderName(folder.getParentFolder().getName())
//                .reason("이동 가능합니다.")
//                .build();
//    }
}
