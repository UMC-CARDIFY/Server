package com.umc.cardify.domain.ProseMirror;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.cardify.dto.card.CardRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attr {
    @NotNull
    Integer level;
    @NotNull
    String question_front;
    @NotNull
    String question_back;
    @NotNull
    List<String> answer;
    @NotNull
    String color;
    @NotNull
    String backgroundColor;

    Long baseImageWidth;

    Long baseImageHeight;

    List<CardRequest.addImageCardOverlay> overlays;
}
