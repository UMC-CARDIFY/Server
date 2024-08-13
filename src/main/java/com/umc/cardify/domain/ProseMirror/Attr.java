package com.umc.cardify.domain.ProseMirror;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attr {
    @NotNull
    Integer level;
    @NotNull
    String question;
    @NotNull
    String answer;
    @NotNull
    String color;
    @NotNull
    String backgroundColor;
}
