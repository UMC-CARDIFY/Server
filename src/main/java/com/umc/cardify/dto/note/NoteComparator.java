package com.umc.cardify.dto.note;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.enums.MarkStatus;
import java.util.Comparator;

public class NoteComparator implements Comparator<Note> {
    private final String order;

    public NoteComparator(String order) {
        this.order = order;
    }

    @Override
    public int compare(Note n1, Note n2) {
        // 1. 북마크 상태 우선 비교 (북마크된 것이 항상 상단)
        int markStateComparison = Boolean.compare(
                n2.getMarkState().equals(MarkStatus.ACTIVE),
                n1.getMarkState().equals(MarkStatus.ACTIVE)
        );
        if (markStateComparison != 0) {
            return markStateComparison;
        }

        // 2. order 조건에 따른 정렬
        if (order == null || order.isEmpty()) {
            return n2.getEditDate().compareTo(n1.getEditDate()); // 기본 수정일 최신순
        }

        switch (order) {
            case "asc": // 이름 오름차순
                return n1.getName().compareTo(n2.getName());
            case "desc": // 이름 내림차순
                return n2.getName().compareTo(n1.getName());
            case "create-newest":
                return n2.getCreatedAt().compareTo(n1.getCreatedAt());
            case "create-oldest":
                return n1.getCreatedAt().compareTo(n2.getCreatedAt());
            case "edit-newest":
                return n2.getEditDate().compareTo(n1.getEditDate());
            case "edit-oldest":
                return n1.getEditDate().compareTo(n2.getEditDate());
            default:
                return n2.getEditDate().compareTo(n1.getEditDate());
        }
    }
}
