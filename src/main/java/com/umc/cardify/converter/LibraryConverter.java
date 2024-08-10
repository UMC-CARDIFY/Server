package com.umc.cardify.converter;

import com.umc.cardify.domain.*;
import com.umc.cardify.dto.library.LibraryResponse;
import com.umc.cardify.repository.DownloadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LibraryConverter {
    private final DownloadRepository downloadRepository;
    public LibraryResponse.NoteInfoDTO toLibInfo(Library library){
        List<Download> downloadList = downloadRepository.findByLibrary(library);
        int cntDownloadAll = 0;
        int cntDownloadWeek = 0;
        if(downloadList != null) {
            cntDownloadAll = downloadList.size();
            cntDownloadWeek = downloadList.stream()
                    .filter(download -> download.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                    .toList().size();
        }

        Note note = library.getNote();
        User user = note.getFolder().getUser();
        List<String> categoryName = library.getCategoryList().stream()
                .map(libraryCategory -> libraryCategory.getCategory().getName()).toList();
        return LibraryResponse.NoteInfoDTO.builder()
                .userName(user.getName())
                .userImgSrc(null)       //추후 유저 이미지 생성되면 삽입
                .noteId(note.getNoteId())
                .noteName(note.getName())
                .cntCard(note.getCards().size())
                .categoryName(categoryName)
                .cntDownloadWeek(cntDownloadWeek)
                .cntDownloadAll(cntDownloadAll)
                .uploadAt(library.getUploadAt())
                .build();
    }
}
