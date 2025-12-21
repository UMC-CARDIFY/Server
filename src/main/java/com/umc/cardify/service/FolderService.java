package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.domain.enums.SubscriptionStatus;
import com.umc.cardify.dto.folder.FolderComparator;
import com.umc.cardify.dto.folder.FolderRequest;
import com.umc.cardify.dto.folder.FolderResponse;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.FolderRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.umc.cardify.domain.enums.MarkStatus.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteConverter noteConverter;

    public void checkOwnership(User user, Folder folder){
        if (!user.equals(folder.getUser()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
    }

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
        MarkStatus parnetMarkStatus = ACTIVE;

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

        List<String> orderList = Arrays.asList("asc", "desc", "edit-newest", "edit-oldest", "create-newest", "create-oldest");
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
        if (!isSubscribed && folderCount >= 61) { // 무료 사용자 제한
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

        if(folder.getMarkState() == ACTIVE){
            folder.setMarkState(MarkStatus.INACTIVE);
        } else {
            folder.setMarkState(ACTIVE);
        }

        folder.setMarkDate(Timestamp.valueOf(LocalDateTime.now()));
        folderRepository.save(folder);

        return FolderResponse.markFolderResultDTO.builder()
                .markState(folder.getMarkState())
                .isSuccess(true)
                .markDate(folder.getMarkDate())
                .build();
    }

    public FolderResponse.getElementListDTO getElementList(User user, Folder folder){
        Map<MarkStatus, List<FolderResponse.FolderInfoDTO>> folderList = folderRepository.findByParentFolderAndUser(folder, user).stream()
                .map(folder1 -> FolderResponse.FolderInfoDTO.builder()
                        .folderId(folder1.getFolderId())
                        .name(folder1.getName())
                        .color(folder1.getColor())
                        .markState(folder1.getMarkState())
                        .getNoteCount(folder1.getNoteCount())
                        .markDate(folder1.getMarkDate())
                        .editDate(folder1.getEditDate())
                        .createdAt(folder1.getCreatedAt())
                        .build())
                .collect(Collectors.groupingBy(FolderResponse.FolderInfoDTO::getMarkState));

        if(folderList.get(MarkStatus.ACTIVE) != null)
            folderList.get(MarkStatus.ACTIVE).sort(Comparator.comparing(FolderResponse.FolderInfoDTO::getMarkDate).reversed());

        Map<MarkStatus, List<NoteResponse.NoteInfoDTO>> noteList =  noteRepository.findByFolder(folder).stream()
                .map(noteConverter::toNoteInfoDTO)
                .collect(Collectors.groupingBy(NoteResponse.NoteInfoDTO::getMarkState));

        if(noteList.get(MarkStatus.ACTIVE) != null)
            noteList.get(MarkStatus.ACTIVE).sort(Comparator.comparing(NoteResponse.NoteInfoDTO::getMarkAt).reversed());

        return FolderResponse.getElementListDTO.builder()
                .folderId(folder.getFolderId())
                .name(folder.getName())
                .color(folder.getColor())
                .markElementList(FolderResponse.markElementList.builder()
                        .folderList(folderList.get(MarkStatus.ACTIVE))
                        .noteList(noteList.get(MarkStatus.ACTIVE))
                        .build())
                .notMarkElementList(FolderResponse.notMarkElementList.builder()
                        .folderList(folderList.get(MarkStatus.INACTIVE))
                        .noteList(noteList.get(MarkStatus.INACTIVE))
                        .build())
                .build();
    }

    public List<FolderResponse.RecentFolderDTO> getRecentFavoriteFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BadRequestException(ErrorResponseStatus.REQUEST_ERROR));

        List<Folder> folders = folderRepository
                .findTop4ByMarkStateAndUserOrderByMarkDateDesc(MarkStatus.ACTIVE, user);

        return folders.stream()
                .map(folder -> FolderResponse.RecentFolderDTO.builder()
                        .folderId(folder.getFolderId())
                        .name(folder.getName())
                        .color(folder.getColor())
                        .markState(folder.getMarkState())
                        .markDate(folder.getMarkDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")))
                        .noteCount(folder.getNoteCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FolderResponse.ParentFolderListDTO searchParentFolders(Long userId, String keyword, Integer page, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 페이징 설정
        int getPage = (page != null) ? page : 0;
        int getSize = (size != null) ? size : Integer.MAX_VALUE;
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

    public FolderResponse.FolderMoveResultDTO moveSubFolder(Long userId, Long moveFolderId, Long targetParentFolderId) {

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 이동할 하위폴더 검증
        Folder moveFolder = folderRepository.findById(moveFolderId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));

        // 대상 상위폴더 검증
        Folder targetParent = null;
        if (targetParentFolderId != null) {
            targetParent = folderRepository.findById(targetParentFolderId)
                    .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));
        }

        boolean isMovingFolderParent = moveFolder.getParentFolder() == null;
        boolean hasChildren = !moveFolder.getSubFolders().isEmpty();

        if (
                isMovingFolderParent &&      // 상위폴더인데
                        hasChildren &&               // 하위폴더를 가지고 있고
                        targetParent != null         // 다른 상위폴더로 이동하려는 경우
        ) {
            throw new BadRequestException(
                    ErrorResponseStatus.CANNOT_MOVE_PARENT_FOLDER_WITH_CHILDREN
            );
        }

        // 자기 자신으로 이동 방지
        if (targetParent != null && moveFolder.getFolderId().equals(targetParent.getFolderId())) {
            throw new BadRequestException(ErrorResponseStatus.INVALID_MOVE_TARGET);
        }

        // 대상이 메인 탭 or 상위폴더인지 확인
        if (targetParent != null && targetParent.getParentFolder() != null) {
            throw new BadRequestException(ErrorResponseStatus.TARGET_MUST_BE_PARENT_FOLDER);
        }

        // 기존 상위폴더 정보 저장
        Folder previousParent = moveFolder.getParentFolder();

        moveFolder.updateParentFolder(targetParent);
        Folder savedFolder = folderRepository.save(moveFolder);

        return FolderResponse.FolderMoveResultDTO.builder()
                .folderId(savedFolder.getFolderId())
                .folderName(savedFolder.getName())
                .previousParentFolderId(previousParent != null ? previousParent.getFolderId() : null)
                .previousParentFolderName(previousParent != null ? previousParent.getName() : null)
                .newParentFolderId(targetParent != null ? targetParent.getFolderId() : null)
                .newParentFolderName(targetParent != null ? targetParent.getName() : null)
                .editDate(savedFolder.getEditDate())
                .build();
    }

    // 노트 이동 - 전체 폴더 검색
    @Transactional(readOnly = true)
    public List<FolderResponse.FolderParentListDTO> searchFolders(Long userId, String keyword, Integer page, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 페이징 설정
        int getPage = (page != null) ? page : 0;
        int getSize = (size != null) ? size : Integer.MAX_VALUE;
        Pageable pageable = PageRequest.of(getPage, getSize);

        List<Folder> parentFoldersToShow;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 1. 키워드에 매칭되는 모든 폴더들 조회
            Page<Folder> matchedFolders = folderRepository.findFoldersByKeyword(user, keyword.trim(), pageable);

            // 2. 매칭된 폴더들로부터 상위폴더 ID들 추출
            Set<Long> parentFolderIds = matchedFolders.getContent().stream()
                    .map(folder -> folder.getParentFolder() == null ?
                            folder.getFolderId() : folder.getParentFolder().getFolderId())
                    .collect(Collectors.toSet());

            // 3. 해당 상위폴더들을 모든 하위폴더와 함께 조회
            parentFoldersToShow = folderRepository.findParentFoldersWithSubFolders(user, new ArrayList<>(parentFolderIds));

        } else {
            // 검색어가 없는 경우 모든 상위폴더 조회
            Page<Folder> allParentFolders = folderRepository.findAllFolders(user, pageable);
            parentFoldersToShow = allParentFolders.getContent();
        }

        // 4. 즐겨찾기 우선 정렬
        parentFoldersToShow.sort((a, b) -> {
            boolean aHasFavorite = a.getMarkState() == ACTIVE ||
                    a.getSubFolders().stream().anyMatch(sub -> sub.getMarkState() == ACTIVE);
            boolean bHasFavorite = b.getMarkState() == ACTIVE ||
                    b.getSubFolders().stream().anyMatch(sub -> sub.getMarkState() == ACTIVE);

            if (aHasFavorite != bHasFavorite) {
                return aHasFavorite ? -1 : 1;
            }
            return a.getName().compareTo(b.getName());
        });

        // 5. DTO 변환
        List<FolderResponse.FolderParentListDTO> parentFolderInfos = parentFoldersToShow.stream()
                .map(parent -> {
                    // 하위폴더 리스트 생성 (즐겨찾기 우선 정렬)
                    List<FolderResponse.FolderInfoDTO> subFolderInfos = parent.getSubFolders().stream()
                            .sorted((sub1, sub2) -> {
                                if (sub1.getMarkState() != sub2.getMarkState()) {
                                    return sub1.getMarkState() == ACTIVE ? -1 : 1;
                                }
                                return sub1.getName().compareTo(sub2.getName());
                            })
                            .map(sub -> FolderResponse.FolderInfoDTO.builder()
                                    .folderId(sub.getFolderId())
                                    .name(sub.getName())
                                    .color(sub.getColor())
                                    .markState(sub.getMarkState())
                                    .getNoteCount(sub.getNotes().size())
                                    .markDate(sub.getMarkDate())
                                    .editDate(sub.getEditDate())
                                    .createdAt(sub.getCreatedAt())
                                    .build())
                            .collect(Collectors.toList());

                    return FolderResponse.FolderParentListDTO.builder()
                            .parentFolderID(parent.getFolderId())
                            .parentFolderName(parent.getName())
                            .parentFolderColor(parent.getColor())
                            .parentMarkState(parent.getMarkState())
                            .getNoteCount(parent.getNotes().size())
                            .foldersList(subFolderInfos)
                            .build();
                })
                .collect(Collectors.toList());

        return parentFolderInfos;
    }

    // 노트 이동 - 노트 이동 API(상위폴더로 이동할 수 있고 하위폴더로 이동할 수 있음)
    @Transactional
    public NoteResponse.NoteMoveResultDTO moveNote(Long userId, Long noteId, Long targetFolderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        // 노트 조회
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_EXIST_NOTE));

        // 이전 폴더
        Folder previousFolder = note.getFolder();

        // 이동할 폴더 검증
        Folder targetFolder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));

        // 동일 폴더 이동 방지
        if (previousFolder.getFolderId().equals(targetFolderId)) {
            throw new BadRequestException(ErrorResponseStatus.ALREADY_IN_TARGET_FOLDER);
        }

        // 폴더 변경
        note.setFolder(targetFolder);
        Note savedNote = noteRepository.save(note);

        return NoteResponse.NoteMoveResultDTO.builder()
                .noteId(note.getNoteId())
                .noteName(note.getName())
                .previousFolderId(previousFolder.getFolderId())
                .previousFolderName(previousFolder.getName())
                .newFolderId(targetFolder.getFolderId())
                .newFolderName(targetFolder.getName())
                .editDate(savedNote.getEditDate())
                .build();
    }
}
