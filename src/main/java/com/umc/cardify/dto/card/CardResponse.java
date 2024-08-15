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
	@Schema(title = "CARD_RES_02 : 플래시 카드 리스트 조회 DTO")
	@Builder
	public static class getCardLists {
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
	}
}
