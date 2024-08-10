package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.*;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteConverter noteConverter;
    private final CardRepository cardRepository;
    private final CardService cardService;

    private final LibraryRepository libraryRepository;
    private final CategoryRepository categoryRepository;
    private final LibraryCategoryRepository libraryCategoryRepository;
    public Note getNoteToID(long noteId){
        return noteRepository.findById(noteId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
    }
    public Note addNote(Folder folder, Long userId){
        if(!userId.equals(folder.getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            Note newNote = NoteConverter.toAddNote(folder);
            return noteRepository.save(newNote);
        }
    }

    public NoteResponse.NoteListDTO getNotesByUserId(Long userId, Integer page, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

        int getNotePage = (page != null) ? page : 0;
        int getNoteSize = (size != null) ? size : 30;

        Pageable pageable = PageRequest.of(getNotePage, getNoteSize);
        Page<Note> notePage = noteRepository.findByUser(user, pageable);

        if(notePage == null){
            throw new DatabaseException(ErrorResponseStatus.NOT_EXIST_NOTE);
        }

        List<NoteResponse.NoteInfoDTO> notes = notePage.getContent().stream()
                .map(note -> NoteResponse.NoteInfoDTO.builder()
                        .noteId(note.getNoteId())
                        .name(note.getName())
                        .folderName(note.getFolder().getName())
                        .markState(note.getMarkState())
                        .editDate(note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .createdAt(note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .build())
                .collect(Collectors.toList());

        return NoteResponse.NoteListDTO.builder()
                .noteList(notes)
                .listsize(notePage.getSize())
                .currentPage(notePage.getNumber()+1)
                .totalPage(notePage.getTotalPages())
                .totalElements(notePage.getTotalElements())
                .isFirst(notePage.isFirst())
                .isLast(notePage.isLast())
                .build();
    }
    public Boolean deleteNote(Long noteId, Long userId){
        Note note_del = noteRepository.findById(noteId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        if(!userId.equals(note_del.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            noteRepository.delete(note_del);
            return true;
        }
    }

    public NoteResponse.GetNoteToFolderResultDTO getNoteToFolder(Folder folder, NoteRequest.GetNoteToFolderDto request){
        Pageable pageable;

        Integer page = request.getPage();
        if(page == null) page = 0;

        Integer size = request.getSize();
        if(size == null) size = folder.getNotes().size();

        String order = request.getOrder();
        if(order == null) order = "asc";

        switch (order.toLowerCase()) {
            case "asc":
                pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("name")));
                break;
            case "desc":
                pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("name")));
                break;
            case "edit-newest":
                pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("editDate")));
                break;
            case "edit-oldest":
                pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("editDate")));
                break;
            default:
                throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }
        Page<Note> notes_all = noteRepository.findByFolder(folder, pageable);

        NoteResponse.GetNoteToFolderResultDTO noteDTO = noteConverter.toGetNoteToFolderResult(folder, notes_all);
        return noteDTO;
    }
    public Boolean markNote(Long noteId, Boolean isMark, Long userId){
        Note note_mark = noteRepository.findById(noteId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        if(!userId.equals(note_mark.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            if (isMark) {
                note_mark.setMarkState(MarkStatus.ACTIVE);
                note_mark.setMarkAt(LocalDateTime.now());
            }
            else if (!isMark){
                note_mark.setMarkState(MarkStatus.INACTIVE);
                note_mark.setMarkAt(null);
            }
            else
                throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
            noteRepository.save(note_mark);
            return true;
        }
    }
    public Boolean writeNote(NoteRequest.WriteNoteDto request, Long userId){
        Note note = noteRepository.findById(request.getNoteId()).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        if(!(userId.equals(note.getFolder().getUser().getUserId())))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else if(libraryRepository.findByNote(note) != null){
            throw new BadRequestException(ErrorResponseStatus.DB_INSERT_ERROR);
        }
        else {
            note.setName(request.getName());

            //저장되어 있는 노트 내용과 입력된 내용이 같을 시 카드를 저장하지 않음
            if(!note.getContents().equals(request.getContents())) {
                note.setContents(request.getContents());
                //여기에 카드 작성이 들어가 있었음
            }
            noteRepository.save(note);

            return true;
        }
    }
    public List<NoteResponse.SearchNoteResDTO> searchNote(Folder folder, String search){
        List<Note> notes = noteRepository.findByFolder(folder).stream()
                .filter(note -> note.getContents().contains(search) || note.getName().contains(search))
                .toList();
        List<NoteResponse.SearchNoteResDTO> searchList = notes.stream()
                .map(list->noteConverter.toSearchNoteResult(list, search))
                .collect(Collectors.toList());

        return searchList;
    }
    public Boolean shareLib(Long userId, NoteRequest.ShareLibDto request) {
        Note note = getNoteToID(request.getNoteId());
        if(!userId.equals(note.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        if(note.getDownloadLibId() != null)
            throw new BadRequestException(ErrorResponseStatus.DB_INSERT_ERROR);
        //기존에 공유되어 있던 데이터를 삭제
        Library library = note.getLibrary();
        if(library != null){
            note.setLibrary(null);
            libraryRepository.delete(library);
        }

        Library library_new = Library.builder()
                .note(note)
                .uploadAt(LocalDateTime.now())
                .build();
        libraryRepository.save(library_new);

        //카테고리 찾아서 Library에 삽입
        List<Category> categoryList = null;
        if(request.getCategory().size() > 0 && request.getCategory().size() <= 3) {
            categoryList = request.getCategory().stream()
                    .map(categoryStr -> {
                        Category category = categoryRepository.findByName(categoryStr);
                        //요청한 카테고리가 없으면 에러
                        if(category == null)
                            throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_CATEGORY);
                        return category;
                    })
                    .toList();
        }
        else
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        if(categoryList != null){
            categoryList.stream()
                    .map(category -> libraryCategoryRepository.save(LibraryCategory.builder()
                            .category(category)
                            .library(library_new)
                            .build()))
                    .toList();
        }

        noteRepository.save(note);
        return true;
    }
    public Boolean cancelShare(Long noteId, Long userId){
        Note note = getNoteToID(noteId);
        if(!userId.equals(note.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        Library library = note.getLibrary();

        note.setLibrary(null);
        noteRepository.save(note);

        if(library!=null)
            libraryRepository.delete(library);
        return true;
    }
    public NoteResponse.getNoteDTO getNote(Long noteId){
        Note note = noteRepository.findById(noteId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        List<NoteResponse.getNoteCardDTO> cardDTO = note.getCards().stream()
                .map(card -> {
                    return NoteResponse.getNoteCardDTO.builder()
                            .cardId(card.getCardId())
                            .cardName(card.getName())
                            .contentsFront(card.getContentsFront())
                            .contentsBack(card.getContentsBack())
                            .build();
                })
                .toList();
        return NoteResponse.getNoteDTO.builder()
                .noteId(note.getNoteId())
                .noteName(note.getName())
                .noteContent(note.getContents())
                .cardList(cardDTO)
                .build();
    }
}
