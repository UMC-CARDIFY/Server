package com.umc.cardify.converter;

import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NoteConverter {
    public static Note toAddNote(Folder folder){
        return Note.builder()
                .folder(folder)
                .name("제목없음")
                .contents("빈 노트")
                .isEdit(false)
                .build();
    }
    public static NoteResponse.AddNoteResultDTO toAddNoteResult(Note note){
        return NoteResponse.AddNoteResultDTO.builder()
                .noteId(note.getNoteId())
                .createdAt(LocalDateTime.now())
                .build();
    }
    public static NoteResponse.ShareResultDTO toShareResult(Note note){
        return NoteResponse.ShareResultDTO.builder()
                .uuid(note.getNoteUUID().toString())
                .build();
    }
    public static NoteResponse.SearchUUIDResultDTO toSearchUUIDResult(Note note){
        return NoteResponse.SearchUUIDResultDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .contents(note.getContents())
                .isEdit(note.getIsEdit())
                .build();
    }

    public NoteResponse.NoteInfoDTO toNoteInfoDTO(Note note) {
        return NoteResponse.NoteInfoDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .folderName(note.getFolder().getName())
                .markState(note.getMarkState())
                .editDate(note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .createdAt(note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .build();
    }
    public static NoteResponse.IsSuccessNoteDTO isSuccessNoteResult(Boolean isSuccess){
        return NoteResponse.IsSuccessNoteDTO.builder()
                .isSuccess(isSuccess)
                .build();
    }
    public static NoteResponse.NoteInfoDTO SearchNoteDTO(Note note) {
        return NoteResponse.NoteInfoDTO.builder()
                .noteId(note.getNoteId())
                .name(note.getName())
                .markState(note.getMarkState())
                .folderName(note.getFolder().getName())
                .editDate(note.getEditDate().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .createdAt(note.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .build();
    }
    public static NoteResponse.GetNoteToFolderResultDTO toGetNoteToFolderResult(Folder folder, Page<Note> notePage){
        List<NoteResponse.NoteInfoDTO> noteResult= notePage.stream()
                .map(NoteConverter::SearchNoteDTO).collect(Collectors.toList());
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
}
