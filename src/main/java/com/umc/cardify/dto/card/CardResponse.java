package com.umc.cardify.dto.card;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.umc.cardify.domain.enums.MarkStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class CardResponse {
	@Getter
	@Schema(title = "CARD_RES_01 : 이미지 카드 조회 DTO")
	@Builder
	public static class getImageCard {
		@Schema(description = "배경 이미지")
		String imgUrl;

		@Schema(description = "이미지 너비", example = "800")
		Long baseImageWidth;

		@Schema(description = "이미지 높이", example = "600")
		Long baseImageHeight;

		@Schema(description = "가림판 배열")
		List<CardRequest.addImageCardOverlay> overlays;

		@Schema(description = "이미지 카드 id")
		Long imageCardId;

		@Schema(description = "노트 id")
		Long noteId;

		@Schema(description = "폴더 id")
		Long folderId;

		@Schema(description = "카드 타입")
		String cardType;
	}

	@Getter
	@Schema(title = "CARD_RES_02 : 플래시 카드 메인 화면 조회 DTO")
	@Builder
	public static class getStudyCardSetLists {
		@Schema(description = "학습 상태")
		int studyStatus;

		@Schema(description = "노트(카드 묶음) 제목 - 굵은 글씨")
		String noteName;

		@Schema(description = "노트(카드 묶음) 색상")
		String color;

		@Schema(description = "폴더 제목 - 작은 글씨")
		String folderName;

		@Schema(description = "최근 학습 시간")
		LocalDateTime recentStudyDate;

		@Schema(description = "다음 학습 시간")
		LocalDateTime nextStudyDate;

		@Schema(description = "학습 카드셋 id")
		Long studyCardSetId;

		@Schema(description = "즐겨찾기 여부")
		MarkStatus markStatus;
	}

	@Getter
	@Schema(title = "CARD_RES_03 : 일반 학습 DTO (카드 리스트)")
	@Builder
	public static class getCardLists {
		@Schema(description = "빈칸 앞")
		String contentsFront;

		@Schema(description = "빈칸 뒤 (빈칸 카드만 존재)")
		String contentsBack;

		@Schema(description = "정답")
		String answer;

		@Schema(description = "카드 id")
		Long cardId;

		@Schema(description = "노트 id")
		Long noteId;

		@Schema(description = "폴더 id")
		Long folderId;

		@Schema(description = "카드 타입")
		String cardType;

		@Schema(description = "멀티카드 정답")
		List<String> multiAnswer;
	}

	@Getter
	@Schema(title = "CARD_RES_04 : 카드 학습 그래프 DTO")
	@Builder
	public static class cardStudyGraph {
		@Schema(description = "쉬움 카드 학습 개수")
		int easyCardsNumber;

		@Schema(description = "알맞음 카드 학습 개수")
		int normalCardsNumber;

		@Schema(description = "어려움 카드 학습 개수")
		int hardCardsNumber;

		@Schema(description = "패스 카드 학습 개수")
		int passCardsNumber;

		@Schema(description = "쉬움 카드 학습 비율")
		int easyCardsPercent;

		@Schema(description = "쉬움 카드 학습 비율")
		int normalCardsPercent;

		@Schema(description = "쉬움 카드 학습 비율")
		int hardCardsPercent;

		@Schema(description = "쉬움 카드 학습 비율")
		int passCardsPercent;
	}

	@Getter
	@Schema(title = "CARD_RES_05 : 분석 학습 기록 조회 DTO")
	@Builder
	public static class getStudyLog {
		@Schema(description = "학습한 카드 개수")
		int cardNumber;

		@Schema(description = "학습 일자")
		LocalDateTime studyDate;
	}

	@Getter
	@Schema(title = "CARD_RES_06 : 분석 학습 제안 DTO")
	@Builder
	public static class getStudySuggestion {
		@Schema(description = "남은 시간")
		String remainTime;

		@Schema(description = "카드 (가 속한 노트) 이름")
		String noteName;

		@Schema(description = "카드 (가 속한 폴더) 이름")
		String folderName;

		@Schema(description = "카드 id")
		Long cardId;

		@Schema(description = "카드 유형 (CARD 또는 IMAGE_CARD)")
		String cardType;

		@Schema(description = "폴더 색상")
		String color;

		@Schema(description = "다음 학습 날짜")
		String date;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "CARD_RES_07 : 주간 학습 카드 개수 반환 DTO")
	public static class weeklyResultDTO {
		@Schema(description = "이번 주 학습한 카드 개수")
		long thisWeekCardCount;
		@Schema(description = "이번주 날짜별 학습한 카드 개수")
		Map<Integer, Long> dayOfThisWeekCard;
		@Schema(description = "지난주 날짜별 학습한 카드 개수")
		Map<Integer, Long> dayOfLastWeekCard;
	}


	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(title = "CARD_RES_08 : 학습 필요 날 반환 DTO")
	public static class getExpectedStudyDateDTO {
		@Schema(description = "학습 예상 날짜")
		List<Integer> expectedDate;
	}

	@Data
	@AllArgsConstructor
	@Schema(title = "CARD_RES_09 : 연간 학습 기여도 리스트 DTO")
	public static class AnnualResultDTO {
		@Schema(description = "연간 모든 날짜에 대한 정보 리스트")
		private List<DailyContribution> contributions;
		@Schema(description = "최대 연속 일수", example = "2")
		private int maxStreak;
	}

	@Data
	@AllArgsConstructor
	@Schema(title = "CARD_RES_10 : 일일 기여도 조회 DTO")
	public static class DailyContribution {
		@Schema(description = "날짜", example = "2025-01-01")
		private LocalDate date;
		@Schema(description = "중복없는 하루 학습 횟수", example = "3")
		private long count;
		@Schema(description = "색상", example = "transparent | light | medium | dark")
		private String color;
	}
}
