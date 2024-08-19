package com.umc.cardify.dto.card;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

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
	}

	@Getter
	@Schema(title = "CARD_RES_02 : 플래시 카드 메인 화면 조회 DTO")
	@Builder
	public static class getStudyCardSetLists {
		@Schema(description = "학습 상태")
		String studyStatus;

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
	public static class getStudyLog{
		@Schema(description = "학습한 카드 개수")
		int cardNumber;

		@Schema(description = "학습 일자")
		LocalDateTime studyDate;
	}
}
