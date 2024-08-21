package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.MarkStatus;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@DynamicUpdate
@DynamicInsert
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Note extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String name;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String totalText;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(15) DEFAULT 'INACTIVE'")
    private MarkStatus markState;

    @Setter
    private LocalDateTime markAt;

    @Setter
    private LocalDateTime viewAt;

    @UpdateTimestamp
    private Timestamp editDate;

    @Setter
    private Long downloadLibId;

    @Setter
    private Boolean isEdit;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL)
    private List<Card> cards;

    @Setter
    @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
    private Library library;

    @Setter
    @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
    private ContentsNote contentsNote;
}
