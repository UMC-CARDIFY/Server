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
    public LibraryResponse.LibInfoDTO toLibInfo(Library library, Long userId){
        List<Download> userDownload;
        Boolean isDownload = false;

        List<Download> downloadList = downloadRepository.findByLibrary(library);
        int cntDownloadAll = 0;
        int cntDownloadWeek = 0;
        if(downloadList != null) {
            cntDownloadAll = downloadList.size();
            cntDownloadWeek = downloadList.stream()
                    .filter(download -> download.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                    .toList().size();
            userDownload = downloadList.stream().filter(download -> download.getUser().getUserId().equals(userId))
                    .toList();
            if(!userDownload.isEmpty())
                isDownload = true;
        }

        Note note = library.getNote();
        User user = note.getFolder().getUser();
        List<String> categoryName = library.getCategoryList().stream()
                .map(libraryCategory -> libraryCategory.getCategory().getName()).toList();
        return LibraryResponse.LibInfoDTO.builder()
                .libraryId(library.getLibraryId())
                .userName(user.getName())
                .userImgSrc(user.getProfileImage())
                .noteId(note.getNoteId())
                .noteName(note.getName())
                .cntCard(note.getCards().size())
                .categoryName(categoryName)
                .isDownload(isDownload)
                .cntDownloadWeek(cntDownloadWeek)
                .cntDownloadAll(cntDownloadAll)
                .uploadAt(library.getUploadAt())
                .build();
    }
}
