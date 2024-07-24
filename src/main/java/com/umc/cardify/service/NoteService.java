package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.domain.enums.Side;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.CardRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    public Note shareNote(Note note, Boolean isEdit, Long userId){
        if(!userId.equals(note.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            if (note.getNoteUUID() == null) {
                note.setNoteUUID(UUID.randomUUID());
            }
            note.setIsEdit(isEdit);
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
    public List<Note> searchNoteMark(Folder folder){
        List<Note> notes = noteRepository.findByFolder(folder);
        List<Note> notes_result = notes.stream()
                .filter(note -> note.getMarkState().equals(MarkStatus.ACTIVE))
                .toList();
        return notes_result;
    }
    public List<Note> searchNoteNotMark(Folder folder){
        List<Note> notes = noteRepository.findByFolder(folder);
        List<Note> notes_result = notes.stream()
                .filter(note -> note.getMarkState().equals(MarkStatus.INACTIVE))
                .toList();
        return notes_result;
    }

    public Boolean markNote(Long noteId, Boolean isMark, Long userId){
        Note note_mark = noteRepository.findById(noteId).orElseThrow(()-> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        if(!userId.equals(note_mark.getFolder().getUser().getUserId()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
        else {
            if (isMark)
                note_mark.setMarkState(MarkStatus.ACTIVE);
            else if (!isMark)
                note_mark.setMarkState(MarkStatus.INACTIVE);
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
            note.setContents(request.getContents());
            noteRepository.save(note);

            List<NoteRequest.WriteCardDto> cardsDto = request.getCards();
            if(note.getCards() != null) {
                List<Card> cardList = cardRepository.findByNote(note);
                cardRepository.deleteAll(cardList);
            }
            if (cardsDto != null) {
                cardsDto.forEach((card) -> addCard(card, note));
            }
            return true;
        }
    }
    public void addCard(NoteRequest.WriteCardDto cardDto, Note note){
        String contents_front = cardDto.getText();
        String contents_back = contents_front
                .replace(">>", "")
                .replace("<<", "")
                .replace("{{", "")
                .replace("}}", "")
                .replace("==", "");

        Card card_front = Card.builder()
                .note(note)
                .name(cardDto.getName())
                .contents(contents_front)
                .side(Side.FRONT)
                .countLearn(0L)
                .build();
        Card card_back = Card.builder()
                .note(note)
                .name(cardDto.getName())
                .contents(contents_back)
                .side(Side.BACK)
                .countLearn(0L)
                .build();

        cardRepository.save(card_front);
        cardRepository.save(card_back);
    }
}
