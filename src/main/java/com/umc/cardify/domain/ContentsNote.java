package com.umc.cardify.domain;

import com.umc.cardify.domain.ProseMirror.Node;
import jakarta.persistence.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "cardify")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContentsNote {
    @Id
    private ObjectId _id;

    @Setter
    private Node contents;

    private Long noteId;
}
