package com.umc.cardify.domain;

import com.umc.cardify.domain.ProseMirror.Node;
import jakarta.persistence.*;
import lombok.*;
import org.bson.types.ObjectId;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.mongodb.core.mapping.Document;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContentsNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contents_id")
    private Long contentsId;

    @Setter
    @Column(columnDefinition = "Text")
    private String contents;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;
}
