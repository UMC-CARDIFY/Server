package com.umc.cardify.dto.note;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.enums.MarkStatus;

import java.util.Comparator;
import java.util.Map;

public class NoteFilterComparator implements Comparator<Note> {
    private final String filter;
    private final Map<Long, Long> cardCountMap;

    public NoteFilterComparator(String filter, Map<Long, Long> cardCountMap) {
        this.filter = filter;
        this.cardCountMap = cardCountMap;
    }

    @Override
    public int compare(Note n1, Note n2) {
        // 1. 북마크 상태 우선 비교
        int markStateComparison = Boolean.compare(n2.getMarkState().equals(MarkStatus.ACTIVE),
                n1.getMarkState().equals(MarkStatus.ACTIVE));
        if (markStateComparison != 0) {
            return markStateComparison;
        }

        // 2. 카드 개수 비교
        Long cardCount1 = cardCountMap.getOrDefault(n1.getNoteId(), 0L);
        Long cardCount2 = cardCountMap.getOrDefault(n2.getNoteId(), 0L);

        int cardComparison;
        if ("card-most".equals(filter)) {
            // 카드 많은 순
            cardComparison = cardCount2.compareTo(cardCount1);
        } else if ("card-less".equals(filter)) {
            // 카드 적은 순
            cardComparison = cardCount1.compareTo(cardCount2);
        } else {
            cardComparison = 0;
        }

        if (cardComparison != 0) {
            return cardComparison;
        }

        // 3. 카드 개수가 같으면 기본값: 수정일 최신순
        return n2.getEditDate().compareTo(n1.getEditDate());
    }
}
