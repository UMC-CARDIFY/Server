package com.umc.cardify.domain;

import com.umc.cardify.domain.enums.MarkStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Folder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", columnDefinition = "varchar(30) NOT NULL")
    private String name;

    @Column(name = "color", columnDefinition = "varchar(10) NOT NULL")
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(15) DEFAULT 'INACTIVE'")
    private MarkStatus markState;

    @UpdateTimestamp
    @Column(name = "mark_date", columnDefinition = "varchar(15) NOT NULL")
    private Timestamp markDate;

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
