package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.*;
import com.umc.cardify.dto.library.LibraryRequest;
import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.repository.*;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private final CardService cardService;
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
    public Boolean downloadLib(Long userId, LibraryRequest.DownloadLibDto request){
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

        List<Download> downloadList = downloadRepository.findByUser(user);
        Download download = null;
        if(!downloadList.isEmpty())
            download = downloadList.stream()
                    .filter(download1 -> download1.getLibrary().getLibraryId().equals(library.getLibraryId()))
                    .findAny().get();
        List<Card> cardList = null;
        Note note_down = library.getNote();
        Integer point_current = user.getPoint();

        if(download == null) {      //다운받은 적 없는 노트일때
            download = Download.builder()
                    .user(user)
                    .library(library)
                    .build();
            if (request.getIsContainCard() && !note_down.getCards().isEmpty()) {
                cardList = note_down.getCards();
                user.setPoint(point_current - 300);
                download.setIsContainCard(true);
            } else {
                user.setPoint(point_current - 200);
                download.setIsContainCard(false);
            }
        }
        else{       //다운받은 적 있는 노트일때
            if(download.getIsContainCard()){    //카드포함으로 다운로드한 적 있을때
                if(request.getIsContainCard())
                    cardList = note_down.getCards();
            }
            else {      //카드미포함으로 다운로드한 적 있을때
                if (request.getIsContainCard() && !note_down.getCards().isEmpty()) {
                    cardList = note_down.getCards();
                    user.setPoint(point_current - 100);
                    download.setIsContainCard(true);
                }
            }
        }
        downloadRepository.save(download);

        Note note_new = Note.builder()
                .folder(folder)
                .name(note_down.getName())
                .contents(note_down.getContents())
                .downloadLibId(library.getLibraryId())
                .build();
        noteRepository.save(note_new);

        if(cardList != null)
            cardList.forEach(card -> cardService.addCard(card, note_new));
        return true;
    }
    public List<LibraryResponse.TopNoteDTO> getTopNote(){
        List<Library> libraryList = libraryRepository.findAll();
        List<LibraryResponse.TopNoteDTO> resultDto = libraryList.stream()
                .map(library -> {
                    int count = downloadRepository.findByLibrary(library).stream()
                            .filter(download -> download.getCreatedAt().compareTo(LocalDateTime.now().minusDays(7)) > 0)
                            .toList().size();
                    User user = library.getNote().getFolder().getUser();
                    List<String> categoryName = library.getCategoryList().stream()
                            .map(libraryCategory -> libraryCategory.getCategory().getName()).toList();
                    return LibraryResponse.TopNoteDTO.builder()
                            .userName(user.getName())
                            .userImgSrc(null)       //추후 유저 이미지 생성되면 삽입
                            .cntDownload(count)
                            .noteName(library.getNote().getName())
                            .cntCard(library.getNote().getCards().size())
                            .categoryName(categoryName)
                            .build();
                })
                .sorted(Comparator.comparing(LibraryResponse.TopNoteDTO::getCntDownload).reversed())
                .collect(Collectors.toList());

        return resultDto;
    }
    public List<LibraryResponse.CategoryInfoDTO> getTopCategory(){
        List<Category> categoryList = categoryRepository.findAll();
        List<LibraryResponse.CategoryInfoDTO> resultCateDTO = categoryList.stream()
                .map(category -> {
                    List<LibraryCategory> uploadList = libraryCategoryRepository.findByCategory(category).stream()
                            .filter(upload->upload.getCreatedAt().compareTo(LocalDateTime.now().minusDays(7)) > 0)
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
    public List<LibraryResponse.TopNoteDTO> getNoteToCategory(String input) {
        Category category = categoryRepository.findByName(input);
        if(category==null)
            throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_CATEGORY);
        List<LibraryResponse.TopNoteDTO> resultList = libraryCategoryRepository.findByCategory(category).stream()
                .map(upload -> {
                    Library library = upload.getLibrary();
                    Note note = library.getNote();
                    User user = note.getFolder().getUser();
                    List<String> categoryName = library.getCategoryList().stream()
                            .map(libraryCategory -> libraryCategory.getCategory().getName()).toList();
                    return LibraryResponse.TopNoteDTO.builder()
                            .userName(user.getName())
                            .userImgSrc(null)       //추후 유저 이미지 생성되면 삽입
                            .noteName(note.getName())
                            .cntCard(note.getCards().size())
                            .categoryName(categoryName)
                            .build();
                })
                .sorted(Comparator.comparing(LibraryResponse.TopNoteDTO::getNoteName))
                .toList();
        return resultList;
    }
}
