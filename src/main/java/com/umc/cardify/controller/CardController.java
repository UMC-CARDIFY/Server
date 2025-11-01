package com.umc.cardify.controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
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
import com.umc.cardify.service.CardComponentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "CardController", description = "카드 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/cards")
public class CardController {

	private final CardComponentService cardComponentService;

	@PostMapping(value = "/add/Image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 카드 생성", description = "이미지 및 가림판들의 크기와 위치 전송")
	public ResponseEntity<String> addImageCard(
			@RequestHeader("Authorization") String token,
			@RequestPart("image") MultipartFile image,
			@RequestPart("imageCard") CardRequest.addImageCard request) {

		String imgUrl = cardComponentService.addImageCard(token, image, request);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping(value = "/view/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 조회", description = "이미지 및 가림판들의 크기와 위치 조회")
	public ResponseEntity<CardResponse.getImageCard> viewImageCard(
			@RequestHeader("Authorization") String token,
			@PathVariable Long imgCardId) {

		return ResponseEntity.ok(cardComponentService.viewImageCard(token, imgCardId));
	}

	@PutMapping(value = "/edit/{imgCardId}/Image")
	@Operation(summary = "이미지 카드 편집", description = "이미지 및 가림판 들의 크기와 위치 조회")
	public ResponseEntity<String> editImageCard(
			@RequestHeader("Authorization") String token,
			@RequestPart("imageCard") CardRequest.addImageCard request,
			@PathVariable Long imgCardId) {

		String imgUrl = cardComponentService.editImageCard(token, request, imgCardId);

		return ResponseEntity.ok(imgUrl);
	}

	@GetMapping("sort-filter")
	@Operation(summary = "플래시 카드 목록 조회(메인 화면) + 정렬, 필터링 기능", description = "유저 노트 중 플래시 카드가 포함된 노트 목록 조회 | 정렬 order = asc, desc, edit-newest, edit-oldest | 필터링 쉼표로 구분된 색상 문자열 입력")
	public ResponseEntity<List<CardResponse.getStudyCardSetLists>> viewStudyCardSetListsBySortFilter(
		@RequestHeader("Authorization") String token, @RequestParam(required = false) String order,
		@RequestParam(required = false) String color, @RequestParam(required = false) Integer studyStatus) {

		List<CardResponse.getStudyCardSetLists> cardListsPage = cardComponentService.getStudyCardSetLists(token, order, color, studyStatus);

		return ResponseEntity.ok(cardListsPage);
	}

	// NOTE : https://www.figma.com/design/BxpTfbBq0G5MxIfy3Nl7X9?node-id=4-2#1450135366 '학습 카드' 기능이 이해 안되면 해당 댓글 참고
	@GetMapping(value = "/{studyCardSetId}")
	@Operation(summary = "학습 카드 - 카드 학습", description = "해당 노트(StudyCardSet)의 학습 카드 전부를 Pageable 리스트로 전달")
	public ResponseEntity<Page<Object>> studyCard(
			@RequestHeader("Authorization") String token,
			@PathVariable Long studyCardSetId,
			@RequestParam(defaultValue = "0") int page) {
		Page<Object> getCardLists = cardComponentService.getCardLists(token, studyCardSetId, page);

		return ResponseEntity.ok(getCardLists);
	}

	@PostMapping("/difficulty")
	@Operation(summary = "학습 카드 - 난이도 선택", description = "해당 학습 카드 학습 후 난이도를 전달")
	public ResponseEntity<?> recordDifficulty(
			@RequestHeader("Authorization") String token,
			@RequestBody CardRequest.difficulty request) {
		cardComponentService.updateCardDifficulty(token, request);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/{cardId}/next-time")
	@Operation(summary = "학습 카드 - 난이도 선택 - 다음 학습 시간 예측 반환", description = "난이도 선택 전에, 각 난이도에 따른 다음 학습시간 예측 반환 | CardType은 일반카드는 0")
	public ResponseEntity<Map<String, LocalDateTime>> getExpectedNextStudyTimes(
			@PathVariable Long cardId,
			@RequestParam int cardType) {

		Map<String, LocalDateTime> result = cardComponentService.getExpectedNextStudyTimes(cardId, cardType);
		return ResponseEntity.ok(result);
	}

	// NOTE : 학습 난이도 선택에 분석학습이 합쳐짐 -- 나중에 개발 기간 끝나고 삭제
//	@GetMapping("{studyCardSetId}/study-completed")
//	@Operation(summary = "분석 학습 완료")
//	public ResponseEntity<?> completeStudy(
//			@RequestHeader("Authorization") String token,
//			@PathVariable Long studyCardSetId) {
//		cardComponentService.completeStudy(token, studyCardSetId);
//
//		return ResponseEntity.ok().build();
//	}

	@GetMapping("{studyCardSetId}/study-graph")
	@Operation(summary = "학습 통계 그래프 조회")
	public ResponseEntity<CardResponse.cardStudyGraph> viewStudyCardGraph(
			@RequestHeader("Authorization") String token,
			@PathVariable Long studyCardSetId) {

		CardResponse.cardStudyGraph cardStudyGraph = cardComponentService.viewStudyCardGraph(token, studyCardSetId);
		return ResponseEntity.ok(cardStudyGraph);
	}


	@GetMapping("{studyCardSetId}/study-log")
	@Operation(summary = "분석 학습 기록 조회")
	public ResponseEntity<?> viewStudyLog(
			@RequestHeader("Authorization") String token, @PathVariable Long studyCardSetId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) {
		Page<CardResponse.getStudyLog> studyLogs = cardComponentService.viewStudyLog(token, studyCardSetId, page, size);
		return ResponseEntity.ok(studyLogs);
	}

	// NOTE : 2025.7월 기준 기능입니다. - '학습 추천' 탭에서 "지금 학습하면 좋은 카드 리스트" 반환
	@PostMapping("/study-suggestion")
	@Operation(summary = "분석 학습 제안")
	public ResponseEntity<List<CardResponse.getStudySuggestion>> suggestionAnalyzeStudy(
			@RequestHeader("Authorization") String token,
		@RequestBody CardRequest.getSuggestion request) {

		Timestamp date = Timestamp.valueOf(LocalDateTime.parse(request.getDate(), DateTimeFormatter.ISO_DATE_TIME));
		List<CardResponse.getStudySuggestion> suggestions = cardComponentService.suggestionAnalyzeStudy(token, date);
		return ResponseEntity.ok(suggestions);
	}

	@DeleteMapping("{studyCardSetId}")
	@Operation(summary = "학습 카드셋 삭제")
	public ResponseEntity<?> deleteStudyCardSet(
			@RequestHeader("Authorization") String token,
			@PathVariable Long studyCardSetId) {
		cardComponentService.deleteStudyCardSet(token, studyCardSetId);

		return ResponseEntity.ok().build();
	}

	// NOTE : 2025.10.21 수정 (parameter studyCardId -> cardId로 변경)
	@GetMapping("{cardId}/re-study")
	@Operation(summary = "재학습")
	public ResponseEntity<?> reStudy(
			@RequestHeader("Authorization") String token,
			@PathVariable Long cardId,
			@RequestParam int cardType) {
		cardComponentService.reStudy(token, cardId, cardType);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/weekly-count")
	@Operation(summary = "주간 학습 결과 API", description = "사용자 조회 성공 시, 해당 주의 총 학습 카드 개수와 날짜별 학습 카드 개수 반환")
	public ResponseEntity<CardResponse.weeklyResultDTO> getCardByWeek(@RequestHeader("Authorization") String token) {

		CardResponse.weeklyResultDTO weekCard = cardComponentService.getCardByWeek(token);
		return ResponseEntity.ok(weekCard);
	}

	@GetMapping("/contributions/{annual}")
	@Operation(summary = "연간 분석 학습 통계 API", description = "현재 연동 입력 후, 사용자의 전체 학습 개수와 1~4단계의 color 반환")
	public ResponseEntity<CardResponse.AnnualResultDTO> getContributionsByAnnual(
			@RequestHeader("Authorization") String token,
			@PathVariable Integer annual) {

		CardResponse.AnnualResultDTO annualResult = cardComponentService.getCardByYear(token, annual);
		return ResponseEntity.ok(annualResult);
	}


	@GetMapping("/study-suggestion/{years}/{month}")
	@Operation(summary = "이번 달 학습 예정 일자")
	public ResponseEntity<?> getExpectedStudyDate(@RequestHeader("Authorization") String token, @PathVariable int years, @PathVariable int month){

		CardResponse.getExpectedStudyDateDTO studyDateDTO = cardComponentService.getExpectedStudyDate(token, years, month);

		return ResponseEntity.ok(studyDateDTO);
	}

	@GetMapping("/quick-learning")
	@Operation(summary = "빠른 학습 탭 - 플래시 카드 세트 조회",
			description = "사용자에게 학습 시간 도달한 카드가 있는 StudyCardSet을 최대 3개 반환")
	public ResponseEntity<List<CardResponse.getExpectedCardSetListDTO>> getQuickLearningStudySets(
			@RequestHeader("Authorization") String token) {

		List<CardResponse.getExpectedCardSetListDTO> sets = cardComponentService.getStudyCardSetsForQuickLearning(token);
		return ResponseEntity.ok(sets);
	}
}
