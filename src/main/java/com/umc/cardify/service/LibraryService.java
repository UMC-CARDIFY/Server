package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.LibraryConverter;
import com.umc.cardify.domain.*;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.dto.library.LibraryRequest;
import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.repository.*;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;
    private final LibraryRepository libraryRepository;
    private final CategoryRepository categoryRepository;
    private final LibraryCategoryRepository libraryCategoryRepository;
    private final DownloadRepository downloadRepository;
    private final ContentsNoteRepository contentsNoteRepository;

    private final CardComponentService cardComponentService;

    private final LibraryConverter libraryConverter;
    public Boolean isUploadLib(Note note){
        Library library = libraryRepository.findByNote(note);
        if(library != null)
            return true;
        else
            return false;
    }
    public List<LibraryResponse.CategoryInfoDTO> getCategory(){
        List<Category> categoryList = categoryRepository.findAll();
        List<LibraryResponse.CategoryInfoDTO> resultDTO = categoryList.stream()
                .map(category -> {
                    int count = libraryCategoryRepository.findByCategory(category).size();
                    return LibraryResponse.CategoryInfoDTO.builder()
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getName())
                            .cntNote(count)
                            .build();
                })
                .collect(Collectors.toList());
        return resultDTO;
    }
    public LibraryResponse.DownloadLibDTO downloadLib(Long userId, LibraryRequest.DownloadLibDto request){
        User user = userRepository.findById(userId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        Folder folder = folderRepository.findById(request.getFolderId()).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        if(!userId.equals(folder.getUser().getUserId())){
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        }
        Library library = libraryRepository.findById(request.getLibraryId()).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));

        //업로더와 같은 유저일 시 오류처리
        if(user.getUserId().equals(library.getNote().getFolder().getUser().getUserId())){
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        }

        Download download = downloadRepository.findByUserAndLibrary(user, library);
        Note note_down = library.getNote();
        Integer point_current = user.getPoint();
        List<Card> cardList = note_down.getCards();
        if(download == null) {      //다운받은 적 없는 노트일때
            download = Download.builder()
                    .user(user)
                    .library(library)
                    .build();
            if (request.getIsEdit()) {
                user.setPoint(point_current - 300);
                download.setIsContainCard(true);
            } else {
                user.setPoint(point_current - 200);
                download.setIsContainCard(false);
            }
        }
        else{       //다운받은 적 있는 노트일때//카드미포함으로 다운로드한 적 있을때
            if (!download.getIsContainCard() && request.getIsEdit()) {
                user.setPoint(point_current - 100);
                download.setIsContainCard(true);
                changeIsEditPossible(user, library.getLibraryId());
            }
        }
        downloadRepository.save(download);

        Note note_new = Note.builder()
                .folder(folder)
                .name(note_down.getName())
                .downloadLibId(library.getLibraryId())
                .totalText(note_down.getTotalText())
                .isEdit(download.getIsContainCard())
                .build();
        noteRepository.save(note_new);

        String contents_down = contentsNoteRepository.findById(note_down.getContentsNote().getContentsId()).get().getContents();
        ContentsNote contentsNote = ContentsNote.builder()
                .contents(contents_down)
                .note(note_new)
                .build();
        contentsNoteRepository.save(contentsNote);

        note_new.setContentsNote(contentsNote);
        noteRepository.save(note_new);

        if(cardList != null)
            cardList.forEach(card -> cardComponentService.addCardToNote(card, note_new));
        return LibraryResponse.DownloadLibDTO.builder()
                .noteId(note_new.getNoteId())
                .build();
    }
    public void changeIsEditPossible(User user, Long libId){
        folderRepository.findByUser(user).forEach(folder -> folder.getNotes().stream()
                .filter(note -> note.getDownloadLibId() != null)
                .filter(note -> note.getDownloadLibId().equals(libId))
                .toList()
                .forEach(note -> {
                    note.setIsEdit(true);
                    noteRepository.save(note);
                }));
    }
    public List<LibraryResponse.LibInfoDTO> getTopNote(Long userId){
        List<Library> libraryList = libraryRepository.findAll();
        List<LibraryResponse.LibInfoDTO> resultDto = libraryList.stream()
                .map(library ->  libraryConverter.toLibInfo(library, userId))
                .sorted(Comparator.comparing(LibraryResponse.LibInfoDTO::getCntDownloadWeek).reversed())
                .collect(Collectors.toList());

        return resultDto;
    }
    public List<LibraryResponse.CategoryInfoDTO> getTopCategory(){
        List<Category> categoryList = categoryRepository.findAll();
        List<LibraryResponse.CategoryInfoDTO> resultCateDTO = categoryList.stream()
                .map(category -> {
                    List<LibraryCategory> uploadList = libraryCategoryRepository.findByCategory(category).stream()
                            .filter(upload-> upload.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                            .sorted(Comparator.comparing(LibraryCategory::getCreatedAt).reversed())
                            .toList();
                    int count = uploadList.size();
                    LocalDateTime uploadAt = LocalDateTime.now().minusDays(7);  //가능한 날짜의 최대값을 초기값으로 설정

                    if(count > 0)
                        uploadAt = uploadList.get(0).getCreatedAt();

                    return LibraryResponse.CategoryInfoDTO.builder()
                            .categoryId(category.getCategoryId())
                            .categoryName(category.getName())
                            .cntNote(count)
                            .uploadAt(uploadAt)
                            .build();
                })
                .sorted(Comparator.comparing(LibraryResponse.CategoryInfoDTO::getUploadAt).reversed())
                .collect(Collectors.toList());
        return resultCateDTO;
    }
    public List<LibraryResponse.LibInfoDTO> getNoteToCategory(String input, String order, Long userId) {
        Category category = categoryRepository.findByName(input);
        Comparator<LibraryResponse.LibInfoDTO> comparator;
        if(category == null)
            throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_CATEGORY);
        comparator = switch (order.toLowerCase()) {
            case "asc" -> Comparator.comparing(LibraryResponse.LibInfoDTO::getNoteName);
            case "desc" -> Comparator.comparing(LibraryResponse.LibInfoDTO::getNoteName).reversed();
            case "upload-newest" -> Comparator.comparing(LibraryResponse.LibInfoDTO::getUploadAt).reversed();
            case "upload-oldest" -> Comparator.comparing(LibraryResponse.LibInfoDTO::getUploadAt);
            case "download" -> Comparator.comparing(LibraryResponse.LibInfoDTO::getCntDownloadAll).reversed();
            default -> throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        };
        List<LibraryResponse.LibInfoDTO> resultList = libraryCategoryRepository.findByCategory(category).stream()
                .map(upload -> libraryConverter.toLibInfo(upload.getLibrary(), userId))
                .sorted(comparator)
                .toList();
        return resultList;
    }
    public LibraryResponse.SearchLibDTO searchLib(LibraryRequest.SearchLibDto request, Long userId){
        String searchTxt;
        if(request.getSearchTxt() == null)
            searchTxt = "";
        else
            searchTxt = request.getSearchTxt();

        List<Library> resultLib;
        List<Category> categoryList;
        if(request.getCategoryList() == null){
            resultLib = libraryCategoryRepository.findAll().stream()
                    .map(LibraryCategory::getLibrary)
                    .toList();
            categoryList = new ArrayList<>();
        }
        else {
            resultLib = new ArrayList<>();
            categoryList = request.getCategoryList().stream()
                    .map(str -> {
                        Category category = categoryRepository.findByName(str);
                        if (category == null)
                            throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_CATEGORY);
                        return category;
                    })
                    .toList();

            categoryList.forEach(category -> {
                List<LibraryCategory> uploadList = libraryCategoryRepository.findByCategory(category);
                List<Library> libList = uploadList.stream()
                        .map(LibraryCategory::getLibrary)
                        .toList();
                resultLib.addAll(libList);
            });
        }

        List<LibraryResponse.LibInfoDTO> resultList = resultLib.stream()
                .distinct()
                .filter(library -> library.getNote().getName().contains(searchTxt))
                .map(library ->  libraryConverter.toLibInfo(library, userId))
                .sorted(Comparator.comparing(LibraryResponse.LibInfoDTO::getCntDownloadWeek).reversed())
                .toList();

        return LibraryResponse.SearchLibDTO.builder()
                .searchTxt(searchTxt)
                .searchCategory(categoryList.stream().map(Category::getName).toList())
                .resultNote(resultList)
                .build();
    }
    public LibraryResponse.CheckDownloadDTO checkDownload(Long userId, Long libraryId){
        User user = userRepository.findById(userId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));;
        Library library = libraryRepository.findById(libraryId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));;
        Download download = downloadRepository.findByUserAndLibrary(user, library);

        String isDownload;

        Note note = library.getNote();
        Folder folder = note.getFolder();

        if(folder.getUser().equals(user)){
            isDownload = "Upload";
        }
        else {
            if (download == null)
                isDownload = "None";
            else if (download.getIsContainCard())
                isDownload = "Edit";
            else
                isDownload = "NotEdit";
        }

        return LibraryResponse.CheckDownloadDTO.builder()
                .folderId(folder.getFolderId())
                .noteId(note.getNoteId())
                .isDownload(isDownload)
                .build();
    }
}
