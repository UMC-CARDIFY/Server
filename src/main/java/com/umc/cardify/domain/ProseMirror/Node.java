package com.umc.cardify.domain.ProseMirror;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node {
    @NotNull
    String type;
    @NotNull
    String text;
    @NotNull
    List<Node> content;
    @NotNull
    Attr attrs;
    @NotNull
    List<Mark> marks;
}
