package com.umc.cardify.dto.library;

import com.umc.cardify.domain.enums.MarkStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.util.List;

public class LibraryResponse {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_01 : 카테고리 조회 DTO")
    public static class CategoryInfoDTO {
        Long categoryId;
        String categoryName;
        Integer cntNote;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_02 : API 실행 성공 여부 응답 DTO")
    public static class IsSuccessLibDTO{
        Boolean isSuccess;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "Library_RES_03 : 추천 노트 조회 DTO")
    public static class TopNoteDTO {
        String noteName;
        List<String> categoryName;
        Integer cntCard;
        String userImgSrc;
        String userName;
        Integer cntDownload;
    }
}
