package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteResponse;
import com.umc.cardify.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoteConverter {
    private final LibraryService libraryService;
    public static Note toAddNote(Folder folder){
        return Note.builder()
                .folder(folder)
                .name("제목없음")
                .viewAt(LocalDateTime.now())
                .build();
    }
    public static NoteResponse.AddNoteResultDTO toAddNoteResult(Note note){
        return NoteResponse.AddNoteResultDTO.builder()
                .noteId(note.getNoteId())
                .createdAt(LocalDateTime.now())
                .build();
    }
    public NoteResponse.NoteInfoDTO toNoteInfoDTO(Note note) {
        return NoteResponse.NoteInfoDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .folderId(note.getFolder().getFolderId())
                .folderColor(note.getFolder().getColor())
                .folderName(note.getFolder().getName())
                .markState(note.getMarkState())
                .editDate(note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .createdAt(note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .isDownload(note.getDownloadLibId() != null)
                .isUpload(libraryService.isUploadLib(note))
                .build();
    }
    public static NoteResponse.IsSuccessNoteDTO isSuccessNoteResult(Boolean isSuccess){
        return NoteResponse.IsSuccessNoteDTO.builder()
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
}