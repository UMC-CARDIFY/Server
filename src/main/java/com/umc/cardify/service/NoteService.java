package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
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
    public Note getNoteToUUID(String uuid_str){
        try{
            UUID uuid = UUID.fromString(uuid_str);
            Note note = noteRepository.findByNoteUUID(uuid);
            if(note == null)
                throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR);
            return note;
        }catch (IllegalArgumentException e){
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }
    }

    public Note addNote(Folder folder, Long userId){
        if(!userId.equals(folder.getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            Note newNote = NoteConverter.toAddNote(folder);
            return noteRepository.save(newNote);
        }
    }

    public NoteResponse.NoteListDTO getNotesByUserId(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Note> notePage = noteRepository.findByUser(user, pageable);

        List<NoteResponse.NoteInfoDTO> notes = notePage.getContent().stream()
                .map(noteConverter::toNoteInfoDTO)
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
    public Note makeLink(Note note, Long userId, Boolean isEdit, Boolean isContainCard){
        if(!userId.equals(note.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            if (note.getNoteUUID() == null) {
                note.setNoteUUID(UUID.randomUUID());
            }
            note.setIsEdit(isEdit);
            note.setIsContainCard(isContainCard);
            return noteRepository.save(note);
        }
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

    public Page<Note> getNoteToFolder(Folder folder, NoteRequest.GetNoteToFolderDto request){
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

        return notes_all;
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
        if(!(userId.equals(note.getFolder().getUser().getUserId()) || note.getIsEdit()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            note.setName(request.getName());

            //저장되어 있는 노트 내용과 입력된 내용이 같을 시 카드를 저장하지 않음
            if(!note.getContents().equals(request.getContents())) {
                note.setContents(request.getContents());
                List<CardRequest.WriteCardDto> cardsDto = request.getCards();
                if (note.getCards() != null) {
                    List<Card> cardList = cardRepository.findByNote(note);
                    cardRepository.deleteAll(cardList);
                }
                if (cardsDto != null) {
                    cardsDto.forEach((card) -> {cardService.addCard(card, note);});
                }
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

        if(libraryRepository.findByNote(note) != null){
            //카테고리 삽입만 시행
        }
        if(libraryRepository.findByNote(note) == null){
            Library library = Library.builder()
                    .note(note)
                    .uploadAt(LocalDateTime.now())
                    .build();
            List<Category> categoryList = null;
            //카테고리 찾아서 Library에 삽입
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
            libraryRepository.save(library);
            if(categoryList != null){
                categoryList.stream()
                        .map(category -> libraryCategoryRepository.save(LibraryCategory.builder()
                            .category(category)
                            .library(library)
                            .build()))
                        .toList();
            }
        }
        note.setIsEdit(request.getIsEdit());
        note.setIsContainCard(request.getIsContainCard());
        noteRepository.save(note);
        return true;
    }
    public Boolean cancelShare(Long noteId, Long userId){
        Note note = getNoteToID(noteId);
        if(!userId.equals(note.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        Library library = note.getLibrary();

        note.setLibrary(null);
        note.setNoteUUID(null);
        note.setIsEdit(null);
        note.setIsContainCard(null);
        noteRepository.save(note);

        if(library!=null)
            libraryRepository.delete(library);
        return true;
    }
}
