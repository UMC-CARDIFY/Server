package com.umc.cardify.domain.ProseMirror;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mark {
    @NotNull
    String type;
    @NotNull
    Attr attrs;
}
