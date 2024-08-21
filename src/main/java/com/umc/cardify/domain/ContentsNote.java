package com.umc.cardify.domain;

import jakarta.persistence.*;
import lombok.*;

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
    @Column(columnDefinition = "JSON")
    private Object contents;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;
}
