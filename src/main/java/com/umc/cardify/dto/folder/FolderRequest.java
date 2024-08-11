package com.umc.cardify.dto.folder;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FolderRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_REQ_01 : 폴더 추가 요청 DTO")
    public static class addFolderDto{
        @NotBlank
        @Schema(description = "폴더 이름", example = "sample")
        String name;
        @NotNull
        @Schema(description = "폴더 색상", example = "blue")
        String color;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "FOLDER_REQ_02 : 폴더 수정 요청 DTO")
    public static class editFolderDto {
        @NotBlank
        @Schema(description = "폴더 이름", example = "기존 폴더 이름")
        String name;
        @NotNull
        @Schema(description = "폴더 색상", example = "기존 색상")
        String color;
    }
}
