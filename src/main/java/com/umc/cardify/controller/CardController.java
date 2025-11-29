package com.umc.cardify.controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Note;
import com.umc.cardify.repository.UserRepository;
import com.umc.cardify.service.NoteService;
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
    private final NoteService noteService;

	@PostMapping(value = "/add/Image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "이미지 카드 생성", description = "이미지 및 가림판들의 크기와 위치 전송")
	public ResponseEntity<String> addImageCard(
			@RequestHeader("Authorization") String token,
			@RequestPart("image") MultipartFile image,
			@RequestPart("imageCard") CardRequest.addImageCard request) {

        Note note = noteService.getNoteById(request.getNoteId());
		String imgUrl = cardComponentService.addImageCard(token, image, request, note);

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

	@GetMapping("{studyCardSetId}/study-graph")
	@Operation(summary = "학습 통계 그래프 조회")
	public ResponseEntity<CardResponse.cardStudyGraph> viewStudyCardGraph(
			@RequestHeader("Authorization") String token,
			@PathVariable Long studyCardSetId) {
		CardResponse.cardStudyGraph cardStudyGraph = cardComponentService.viewStudyCardGraph(token, studyCardSetId);

		return ResponseEntity.ok(cardStudyGraph);
	}

	@GetMapping("{studyCardSetId}/study-completed")
	@Operation(summary = "분석 학습 완료")
	public ResponseEntity<?> completeStudy(
			@RequestHeader("Authorization") String token,
			@PathVariable Long studyCardSetId) {
		cardComponentService.completeStudy(token, studyCardSetId);

		return ResponseEntity.ok().build();
	}

	@GetMapping("{studyCardSetId}/study-log")
	@Operation(summary = "분석 학습 기록 조회")
	public ResponseEntity<?> viewStudyLog(
			@RequestHeader("Authorization") String token, @PathVariable Long studyCardSetId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "4") int size) {
		Page<CardResponse.getStudyLog> studyLogs = cardComponentService.viewStudyLog(token, studyCardSetId, page, size);
		return ResponseEntity.ok(studyLogs);
	}

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

	@GetMapping("{studyCardSetId}/re-study")
	@Operation(summary = "재학습")
	public ResponseEntity<?> reStudy(
			@RequestHeader("Authorization") String token,
			@PathVariable Long studyCardSetId) {
		cardComponentService.reStudy(token, studyCardSetId);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/weekly-count")
	@Operation(summary = "주간 학습 결과 API", description = "사용자 조회 성공 시, 해당 주의 총 학습 카드 개수와 날짜별 학습 카드 개수 반환")
	public ResponseEntity<CardResponse.weeklyResultDTO> getCardByWeek(@RequestHeader("Authorization") String token) {

		CardResponse.weeklyResultDTO weekCard = cardComponentService.getCardByWeek(token);
		return ResponseEntity.ok(weekCard);
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
