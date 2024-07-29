package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.MarkStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private String contents;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(15) DEFAULT 'INACTIVE'")
    private MarkStatus markState;

    @Setter
    private LocalDateTime markAt;

    private LocalDateTime viewAt;

    @UpdateTimestamp
    private Timestamp editDate;

    @Setter
    private UUID noteUUID;

    @Setter
    @Column(columnDefinition = "Boolean DEFAULT false")
    private Boolean isEdit;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL)
    private List<Card> cards;
}
