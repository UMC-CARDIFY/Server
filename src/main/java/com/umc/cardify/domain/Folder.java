package com.umc.cardify.domain;

import com.umc.cardify.config.BaseEntity;
import com.umc.cardify.domain.enums.MarkStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Folder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folderid")
    private Long folderId;

    @Column(name = "name", columnDefinition = "varchar(30) NOT NULL")
    private String name;

    @Column(name = "color", columnDefinition = "varchar(30) NOT NULL")
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(15) DEFAULT 'ACTIVE'")
    private MarkStatus markState;

    @UpdateTimestamp
    private Timestamp editDate;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<Note> notes;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<Note> reviewList = new ArrayList<>();

    @Transient
    public int getNoteCount() {
        return notes != null ? notes.size() : 0;
    }
}
