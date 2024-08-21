package com.umc.cardify.controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.card.CardResponse;
import com.umc.cardify.jwt.JwtUtil;
import com.umc.cardify.service.CardComponentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "CardController", description = "카드 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/cards")
public class CardController {

	private final CardComponentService cardComponentService;

	private final JwtUtil jwtUtil;

	@PostMapping(value = "/add/Image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 카드 생성", description = "이미지 및 가림판들의 크기와 위치 전송")
	public ResponseEntity<String> addImageCard(@RequestPart("image") MultipartFile image,
		@RequestPart("imageCard") CardRequest.addImageCard request) {

		String imgUrl = cardComponentService.addImageCard(image, request);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping(value = "/view/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 조회", description = "이미지 및 가림판들의 크기와 위치 조회")
	public ResponseEntity<CardResponse.getImageCard> viewImageCard(@PathVariable Long imgCardId) {

		return ResponseEntity.ok(cardComponentService.viewImageCard(imgCardId));
	}

	@PutMapping(value = "/edit/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 편집", description = "이미지 및 가림판 들의 크기와 위치 조회")
	public ResponseEntity<String> editImageCard(@RequestPart("imageCard") CardRequest.addImageCard request,
		@PathVariable Long imgCardId) {

		String imgUrl = cardComponentService.editImageCard(request, imgCardId);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping
	@Operation(summary = "플래시 카드 목록 조회(메인 화면)", description = "유저 노트 중 플래시 카드가 포함된 노트 목록 조회")
	public ResponseEntity<Page<CardResponse.getStudyCardSetLists>> viewStudyCardSetLists(
		@RequestHeader("Authorization") String token, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "4") int size) {
		Long userId = jwtUtil.extractUserId(token);

		Pageable pageable = PageRequest.of(page, size);
		Page<CardResponse.getStudyCardSetLists> cardListsPage = cardComponentService.getStudyCardSetLists(userId,
			pageable);

		return ResponseEntity.ok(cardListsPage);
	}

	@GetMapping(value = "/{studyCardSetId}")
	@Operation(summary = "학습 카드 - 카드 학습", description = "해당 노트(StudyCardSet)의 학습 카드 전부를 Pageable 리스트로 전달")
	public ResponseEntity<Page<Object>> studyCard(@PathVariable Long studyCardSetId,
		@RequestParam(defaultValue = "0") int page) {
		Page<Object> getCardLists = cardComponentService.getCardLists(studyCardSetId, page);

		return ResponseEntity.ok(getCardLists);
	}

	@PostMapping("/difficulty")
	@Operation(summary = "학습 카드 - 난이도 선택", description = "해당 학습 카드 학습 후 난이도를 전달")
	public ResponseEntity<?> recordDifficulty(@RequestBody CardRequest.difficulty request) {
		cardComponentService.updateCardDifficulty(request);

		return ResponseEntity.ok().build();
	}

	@GetMapping("{studyCardSetId}/study-graph")
	@Operation(summary = "학습 통계 그래프 조회")
	public ResponseEntity<CardResponse.cardStudyGraph> viewStudyCardGraph(@PathVariable Long studyCardSetId) {
		CardResponse.cardStudyGraph cardStudyGraph = cardComponentService.viewStudyCardGraph(studyCardSetId);

		return ResponseEntity.ok(cardStudyGraph);
	}

	@GetMapping("{studyCardSetId}/study-completed")
	@Operation(summary = "분석 학습 완료")
	public ResponseEntity<?> completeStudy(@PathVariable Long studyCardSetId) {
		cardComponentService.completeStudy(studyCardSetId);

		return ResponseEntity.ok().build();
	}

	@GetMapping("{studyCardSetId}/study-log")
	@Operation(summary = "분석 학습 기록 조회")
	public ResponseEntity<?> viewStudyLog(@PathVariable Long studyCardSetId, @RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "4") int size) {
		Page<CardResponse.getStudyLog> studyLogs = cardComponentService.viewStudyLog(studyCardSetId, page, size);
		return ResponseEntity.ok(studyLogs);
	}

	@GetMapping("study-suggestion")
	@Operation(summary = "분석 학습 제안")
	public ResponseEntity<List<CardResponse.getStudySuggestion>> suggestionAnalyzeStudy(@RequestHeader String token,
		@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		@Parameter(description = "학습을 위한 날짜와 시간", example = "2024-08-20T12:14:56.597Z") String dateString) {
		Long userId = jwtUtil.extractUserId(token);

		Timestamp date = Timestamp.valueOf(LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME));

		List<CardResponse.getStudySuggestion> suggestions = cardComponentService.suggestionAnalyzeStudy(userId, date);

		return ResponseEntity.ok(suggestions);
	}

	@DeleteMapping("{studyCardSetId}")
	@Operation(summary = "학습 카드셋 삭제")
	public ResponseEntity<?> deleteStudyCardSet(@PathVariable Long studyCardSetId){
		cardComponentService.deleteStudyCardSet(studyCardSetId);

		return ResponseEntity.ok().build();
	}

	@GetMapping("{studyCardSetId}/re-study")
	@Operation(summary = "재학습")
	public ResponseEntity<?> reStudy(@PathVariable Long studyCardSetId){
		cardComponentService.reStudy(studyCardSetId);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/weekly-count")
	@Operation(summary = "주간 학습 결과 API", description = "사용자 조회 성공 시, 해당 주의 총 학습 카드 개수와 날짜별 학습 카드 개수 반환")
	public ResponseEntity<CardResponse.weeklyResultDTO> getCardByWeek(
			@RequestHeader("Authorization") String token) {
		Long userId = jwtUtil.extractUserId(token);
		CardResponse.weeklyResultDTO weekCard = cardComponentService.getCardByWeek(userId);
		return ResponseEntity.ok(weekCard);
	}

}
