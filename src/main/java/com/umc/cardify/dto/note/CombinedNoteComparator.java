package com.umc.cardify.dto.note;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.enums.MarkStatus;

import java.util.Comparator;
import java.util.Map;

public class CombinedNoteComparator implements Comparator<Note> {
    private final String order;
    private final String filter;
    private final Map<Long, Long> cardCountMap;

    public CombinedNoteComparator(String order, String filter, Map<Long, Long> cardCountMap) {
        this.order = order;
        this.filter = filter;
        this.cardCountMap = cardCountMap;
    }

    @Override
    public int compare(Note n1, Note n2) {
        // 1. 북마크 상태 우선 비교 (항상 최우선)
        int markStateComparison = Boolean.compare(n2.getMarkState().equals(MarkStatus.ACTIVE),
                n1.getMarkState().equals(MarkStatus.ACTIVE));
        if (markStateComparison != 0) {
            return markStateComparison;
        }

        // 2. filter 조건 적용 (카드 개수)
        if (filter != null && ("card-most".equals(filter) || "card-less".equals(filter))) {
            Long cardCount1 = cardCountMap.getOrDefault(n1.getNoteId(), 0L);
            Long cardCount2 = cardCountMap.getOrDefault(n2.getNoteId(), 0L);

            int cardComparison;
            if ("card-most".equals(filter)) {
                cardComparison = cardCount2.compareTo(cardCount1);
            } else {
                cardComparison = cardCount1.compareTo(cardCount2);
            }

            if (cardComparison != 0) {
                return cardComparison;
            }
        }

        // 3. order 조건 적용
        if (order != null && !order.isEmpty()) {
            switch (order) {
                case "asc":
                    return n1.getName().compareTo(n2.getName());
                case "desc":
                    return n2.getName().compareTo(n1.getName());
                case "create-newest":
                    return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                case "create-oldest":
                    return n1.getCreatedAt().compareTo(n2.getCreatedAt());
                case "edit-newest":
                    return n2.getEditDate().compareTo(n1.getEditDate());
                case "edit-oldest":
                    return n1.getEditDate().compareTo(n2.getEditDate());
            }
        }

        // 4. 기본값: 수정일 최신순
        return n2.getEditDate().compareTo(n1.getEditDate());
    }
}
