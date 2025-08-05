package com.umc.cardify.dto.note;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.enums.MarkStatus;

import java.util.Comparator;
import java.util.Map;

public class NoteComparator implements Comparator<Note> {
    private final String order;
    private Map<Long, Long> cardCountMap;

    public NoteComparator(String order) {
        this.order = order;
    }

    public NoteComparator(String order, Map<Long, Long> cardCountMap) {
        this.order = order;
        this.cardCountMap = cardCountMap;
    }

    @Override
    public int compare(Note n1, Note n2) {
        // 1. 북마크 상태 우선 비교 (북마크된 것이 항상 상단)
        int markStateComparison = Boolean.compare(n2.getMarkState().equals(MarkStatus.ACTIVE),
                n1.getMarkState().equals(MarkStatus.ACTIVE));
        if (markStateComparison != 0) {
            return markStateComparison;
        }

        // 2. order 조건에 따른 정렬
        if (order == null || order.isEmpty()) {
            // 기본값: 수정일 최신순
            return n2.getEditDate().compareTo(n1.getEditDate());
        }

        switch (order) {
            case "asc":
                // 가나다ABC 오름차순
                return n1.getName().compareTo(n2.getName());
            case "desc":
                // 가나다ABC 내림차순
                return n2.getName().compareTo(n1.getName());
            case "create-newest":
                // 생성일 최신순
                return n2.getCreatedAt().compareTo(n1.getCreatedAt());
            case "create-oldest":
                // 생성일 오래된 순
                return n1.getCreatedAt().compareTo(n2.getCreatedAt());
            case "edit-newest":
                // 수정일 최신순
                return n2.getEditDate().compareTo(n1.getEditDate());
            case "edit-oldest":
                // 수정일 오래된 순
                return n1.getEditDate().compareTo(n2.getEditDate());
            default:
                // 기본값: 수정일 최신순
                return n2.getEditDate().compareTo(n1.getEditDate());
        }
    }
}
