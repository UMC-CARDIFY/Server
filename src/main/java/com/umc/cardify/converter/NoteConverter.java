package com.umc.cardify.converter;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.ContentsNote;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.repository.ContentsNoteRepository;
import com.umc.cardify.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoteConverter {
    private final LibraryService libraryService;
    private final ContentsNoteRepository contentsNoteRepository;
    public static Note toAddNote(Folder folder){
        return Note.builder()
                .folder(folder)
                .name("제목없음")
                .viewAt(LocalDateTime.now())
                .totalText(".")
                .isEdit(true)
                .build();
    }
    public static NoteResponse.AddNoteResultDTO toAddNoteResult(Note note){
        return com.umc.cardify.dto.note.NoteResponse.AddNoteResultDTO.builder()
                .noteId(note.getNoteId())
                .createdAt(LocalDateTime.now())
                .build();
    }
    public NoteResponse.NoteInfoDTO toNoteInfoDTO(Note note) {
        // 기본 빌더 시작
        NoteResponse.NoteInfoDTO.NoteInfoDTOBuilder builder = NoteResponse.NoteInfoDTO.builder()
            .noteId(note.getNoteId())
            .name(note.getName())
            .markState(note.getMarkState())
            .flashCardCount(note.getCards() != null ? (long) note.getCards().size() : 0L);

        // 폴더 관련 필드 안전하게 처리
        if (note.getFolder() != null) {
            builder.folderId(note.getFolder().getFolderId())
                .folderColor(note.getFolder().getColor())
                .folderName(note.getFolder().getName());
        } else {
            // 폴더가 null인 경우 기본값 설정
            builder.folderId(null)
                .folderColor(null)
                .folderName(null);
        }

        // 날짜 필드 안전하게 처리
        if (note.getMarkAt() != null) {
            builder.markAt(note.getMarkAt().toLocalDate().format(DateTimeFormatter.ofPattern("yy/MM/dd")));
        } else {
            builder.markAt(null);
        }

        if (note.getEditDate() != null) {
            builder.editDate(note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            builder.editDate(null);
        }

        if (note.getCreatedAt() != null) {
            builder.createdAt(note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            builder.createdAt(null);
        }

        // 다운로드 상태
        builder.isDownload(note.getDownloadLibId() != null);

        // 업로드 상태 - 예외가 발생할 수 있는 메서드 호출을 try-catch로 감싸기
        try {
            builder.isUpload(libraryService.isUploadLib(note));
        } catch (Exception e) {
            // 문제 발생 시 기본값으로 설정하고 로그 기록
            builder.isUpload(false);
            System.err.println("Upload status check failed for note ID: " + note.getNoteId() + " - " + e.getMessage());
        }

        return builder.build();
    }
    public static NoteResponse.IsSuccessNoteDTO isSuccessNoteResult(Boolean isSuccess){
        return com.umc.cardify.dto.note.NoteResponse.IsSuccessNoteDTO.builder()
                .isSuccess(isSuccess)
                .build();
    }
    public NoteResponse.GetNoteToFolderResultDTO toGetNoteToFolderResult(Folder folder, Page<Note> notePage){
        List<NoteResponse.NoteInfoDTO> noteResult= notePage.stream()
                .map(this::toNoteInfoDTO).collect(Collectors.toList());
        return NoteResponse.GetNoteToFolderResultDTO.builder()
                .folderName(folder.getName())
                .folderColor(folder.getColor())
                .noteList(noteResult)
                .listSize(notePage.getSize())
                .currentPage(notePage.getNumber()+1)
                .totalPage(notePage.getTotalPages())
                .totalElements(notePage.getTotalElements())
                .isFirst(notePage.isFirst())
                .isLast(notePage.isLast())
                .build();
    }
    public NoteResponse.SearchNoteResDTO toSearchNoteResult(Note note, String search){
        List<String> textList = new ArrayList<>();
        String text = note.getName() + note.getTotalText();

        while(text.contains(search)){
            int index = text.indexOf(search);

            //분류 기준이 바뀌면 수정
            int moreText = text.indexOf(".", index + search.length());
            if(moreText < 0)
                moreText = text.length();
            textList.add(text.substring(index, moreText));
            text = text.substring(moreText);
        }
        return NoteResponse.SearchNoteResDTO.builder()
                .noteId(note.getNoteId())
                .noteName(note.getName())
                .textList(textList)
                .build();
    }
    public NoteResponse.SearchNoteToUserDTO toSearchNoteUser(Folder folder, List<NoteResponse.SearchNoteResDTO> noteDto){
        //부모 폴더가 없을 때 처리
        Folder pFolder = folder.getParentFolder();

        return NoteResponse.SearchNoteToUserDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getName())
                .parentsFolderId(pFolder==null ? 0L : folder.getParentFolder().getFolderId())
                .parentsFolderName(pFolder==null ? "" : folder.getParentFolder().getName())
                .noteList(noteDto)
                .build();
    }
    public NoteResponse.NoteInfoDTO recentNoteInfoDTO(Note note) {
        return NoteResponse.NoteInfoDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .folderId(note.getFolder().getFolderId())
                .folderColor(note.getFolder().getColor())
                .folderName(note.getFolder().getName())
                .viewAt(note.getViewAt())
                .editDate(note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .createdAt(note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .isDownload(note.getDownloadLibId() != null)
                .isUpload(libraryService.isUploadLib(note))
                .build();
    }
    public NoteResponse.getNoteDTO getNoteDTO(Note note, List<NoteResponse.getNoteCardDTO> cardDTO){
        ContentsNote contentsNote = contentsNoteRepository.findByNote(note).orElseThrow(
                () -> new BadRequestException(ErrorResponseStatus.INVALID_NOTE_TEXT));

        return NoteResponse.getNoteDTO.builder()
                .noteId(note.getNoteId())
                .noteName(note.getName())
                .markState(note.getMarkState().equals(MarkStatus.ACTIVE))
                .noteContent(contentsNote.getContents())
                .isEdit(note.getIsEdit())
                .isUpload(libraryService.isUploadLib(note))
                .cardList(cardDTO)
                .build();
    }

    public NoteResponse.NoteInfoDTO convertToNoteInfoDTO(Note note, Map<Long, Long> cardCountMap) {
        return NoteResponse.NoteInfoDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .folderId(note.getFolder().getFolderId())
                .folderName(note.getFolder().getName())
                .folderColor(note.getFolder().getColor())
                .markState(note.getMarkState())
                .flashCardCount(note.getCards() != null ? (long) note.getCards().size() : 0L)
                .viewAt(note.getViewAt())
                .markAt(note.getMarkAt() != null ? note.getMarkAt().toLocalDate().format(DateTimeFormatter.ofPattern("yy/MM/dd")) : null)
                .editDate(note.getEditDate() != null ? note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null)
                .createdAt(note.getCreatedAt() != null ? note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null)
                .build();
    }

    public NoteResponse.NoteListDTO createEmptyNoteListDTO(int page, int size) {
        return NoteResponse.NoteListDTO.builder()
                .noteList(Collections.emptyList())
                .listsize(size)
                .currentPage(page + 1)
                .totalPage(0)
                .totalElements(0L)
                .isFirst(true)
                .isLast(true)
                .build();
    }

}