package com.umc.cardify.service;

import static com.umc.cardify.config.exception.ErrorResponseStatus.*;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.*;
import com.umc.cardify.domain.enums.AuthProvider;
import com.umc.cardify.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.enums.CardType;
import com.umc.cardify.domain.enums.Difficulty;
import com.umc.cardify.domain.enums.StudyStatus;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.card.CardResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardComponentService {
	private final CardModuleService cardModuleService;
	private final NoteModuleService noteModuleService;
	private final S3Service s3Service;

	private final ImageCardRepository imageCardRepository;
	private final OverlayRepository overlayRepository;
	private final StudyCardSetRepository studyCardSetRepository;
	private final StudyLogRepository studyLogRepository;
	private final StudyHistoryRepository studyHistoryRepository;
	private final UserRepository userRepository;
	private final CardRepository cardRepository;

	private final JwtTokenProvider jwtTokenProvider;

	private Long findUserId(String token) {
		String email = jwtTokenProvider.getEmailFromToken(token.replace("Bearer ", ""));
		AuthProvider provider = jwtTokenProvider.getProviderFromToken(token.replace("Bearer ", "")); // 토큰에 제공자 정보도 포함

		return userRepository.findByEmailAndProvider(email, provider)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID)).getUserId();
	}

	@Transactional
	public CardResponse.getExpectedStudyDateDTO getExpectedStudyDate(String token, int years, int month) {
		Long userId = findUserId(token);

		// 해당 년월의 첫날과 마지막 날 계산
		YearMonth yearMonth = YearMonth.of(years, month);
		LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toLocalDateTime();
		LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();

		// 해당 유저의 해당 기간의 카드를 조회
		List<Card> cards = cardRepository.findAllByUserIdAndLearnNextTimeBetween(userId, startDateTime, endDateTime);
		List<ImageCard> imageCards = imageCardRepository.findAllByUserIdAndLearnNextTimeBetween(userId, startDateTime, endDateTime);

		// 카드를 모두 모아서 learn_next_time의 일자를 추출
		List<Integer> expectedDates = Stream.concat(
				cards.stream().map(card -> card.getLearnNextTime().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate().getDayOfMonth()),
				imageCards.stream().map(imageCard -> imageCard.getLearnNextTime().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate().getDayOfMonth())
			).distinct()
			.sorted()
			.collect(Collectors.toList());

		// DTO에 결과 반환
		return CardResponse.getExpectedStudyDateDTO.builder()
			.expectedDate(expectedDates)
			.build();
	}

	@Transactional
	public void reStudy(String token, Long cardId, int cardType) {
		Long userId = findUserId(token);

		if (cardType == 0) {
			Card card = cardModuleService.getCardById(cardId);
			card.setCountLearn(0L);
			card.setDifficulty(0);
			card.setLearnLastTime(null);
			card.setLearnNextTime(null);
			cardModuleService.saveCard(card);

			// 해당 카드의 학습 로그 삭제
			studyLogRepository.deleteByUser_UserIdAndCard_CardId(userId, cardId);
		} else {
			ImageCard imageCard = cardModuleService.getImageCardById(cardId);
			imageCard.setCountLearn(0L);
			imageCard.setDifficulty(0);
			imageCard.setLearnLastTime(null);
			imageCard.setLearnNextTime(null);
			cardModuleService.saveImageCard(imageCard);

			studyLogRepository.deleteByUser_UserIdAndImageCard_Id(userId, cardId);
		}
	}

	@Transactional
	public void deleteStudyCardSet(String token, Long studyCardSetId) {
		Long userId = findUserId(token);

		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);
		cardModuleService.deleteCardSet(studyCardSetId);
		noteModuleService.deleteNoteById(studyCardSet.getNote().getNoteId());
	}

	@Transactional
	public List<CardResponse.getStudySuggestion> suggestionAnalyzeStudy(String token, Timestamp date) {
		Long userId = findUserId(token);

		List<Card> cards = cardModuleService.findAllCardsByUserIdAndLearnNextTimeOnDate(userId, date);
		List<ImageCard> imageCards = cardModuleService.findAllImageCardsByUserIdAndLearnNextTimeOnDate(userId, date);

		return Stream.concat(cards.stream().map(card -> {
			String remainTime = calculateRemainingTime(card.getLearnNextTime());
			// KST 시간대로 날짜를 변환
			String learnDate = card.getLearnNextTime().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString();

			return CardResponse.getStudySuggestion.builder()
				.remainTime(remainTime)
				.noteName(card.getStudyCardSet().getNote().getName())
				.folderName(card.getStudyCardSet().getFolder().getName())
				.cardId(card.getCardId())
				.cardType("CARD")
				.color(card.getStudyCardSet().getFolder().getColor())
				.date(learnDate) // 학습 날짜 설정
				.build();
		}),imageCards.stream().map(imageCard -> {
			String remainTime = calculateRemainingTime(imageCard.getLearnNextTime());
			// KST 시간대로 날짜를 변환
			String learnDate = imageCard.getLearnNextTime().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString();

			return CardResponse.getStudySuggestion.builder()
				.remainTime(remainTime)
				.noteName(imageCard.getStudyCardSet().getNote().getName())
				.folderName(imageCard.getStudyCardSet().getFolder().getName())
				.cardId(imageCard.getId())
				.cardType("IMAGE_CARD")
				.color(imageCard.getStudyCardSet().getFolder().getColor())
				.date(learnDate) // 학습 날짜 설정
				.build();
		})).collect(Collectors.toList());
	}

	private String calculateRemainingTime(Timestamp learnNextTime) {
		// Timestamp를 LocalDateTime으로 변환하고 9시간을 더함
		LocalDateTime learnTime = learnNextTime.toLocalDateTime().plusHours(9);
		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")); // 한국 표준시(KST)로 현재 시간 설정

		// 현재 시간과 학습 시간 사이의 기간 계산
		Duration duration = Duration.between(currentTime, learnTime);

		// 남은 시간을 시간과 분으로 변환
		long hours = duration.toHours();
		long minutes = duration.toMinutes() % 60;

		return String.format("%02d 시간 %02d 분", hours, minutes);
	}

	@Transactional
	public String addImageCard(String token, MultipartFile image, CardRequest.addImageCard request) {
		Long userId = findUserId(token);
		String imgUrl = s3Service.upload(image, "imageCards");

		ImageCard imageCard = ImageCard.builder()
			.imageUrl(imgUrl)
			.height(request.getBaseImageHeight())
			.width(request.getBaseImageWidth())
			.build();

		Note note = noteModuleService.getNoteById(request.getNoteId());

		StudyCardSet studyCardSet = cardModuleService.findStudyCardSetByNote(note);

		imageCard.setStudyCardSet(studyCardSet);
		cardModuleService.saveStudyCardSet(studyCardSet);

		ImageCard savedImageCard = imageCardRepository.save(imageCard);

		if (request.getOverlays() != null) {
			for (CardRequest.addImageCardOverlay overlayRequest : request.getOverlays()) {
				Overlay overlay = Overlay.builder()
					.xPosition(overlayRequest.getPositionOfX())
					.yPosition(overlayRequest.getPositionOfY())
					.width(overlayRequest.getWidth())
					.height(overlayRequest.getHeight())
					.imageCard(savedImageCard)
					.build();

				overlayRepository.save(overlay);
			}
		}

		return savedImageCard.getImageUrl();
	}

	@Transactional
	public CardResponse.getImageCard viewImageCard(String token, Long imageCardId) {
		Long userId = findUserId(token);

		ImageCard imageCard = imageCardRepository.findById(imageCardId)
			.orElseThrow(() -> new IllegalArgumentException("Image card not found with id: " + imageCardId));

		List<Overlay> overlays = overlayRepository.findByImageCard(imageCard);

		List<CardRequest.addImageCardOverlay> overlayResponses = overlays.stream()
			.map(overlay -> CardRequest.addImageCardOverlay.builder()
				.positionOfX(overlay.getXPosition())
				.positionOfY(overlay.getYPosition())
				.width(overlay.getWidth())
				.height(overlay.getHeight())
				.build())
			.collect(Collectors.toList());

		return CardResponse.getImageCard.builder()
			.imgUrl(imageCard.getImageUrl())
			.baseImageWidth(imageCard.getWidth())
			.baseImageHeight(imageCard.getHeight())
			.overlays(overlayResponses)
			.build();
	}

	@Transactional
	public String editImageCard(String token, CardRequest.addImageCard request, Long imgCardId) {
		Long userId = findUserId(token);

		ImageCard existingImageCard = imageCardRepository.findById(imgCardId)
			.orElseThrow(() -> new IllegalArgumentException("ImageCard not found with ID: " + imgCardId));

		existingImageCard.setHeight(request.getBaseImageHeight());
		existingImageCard.setWidth(request.getBaseImageWidth());

		ImageCard savedImageCard = imageCardRepository.save(existingImageCard);

		overlayRepository.deleteByImageCardId(imgCardId);

		if (request.getOverlays() != null) {
			for (CardRequest.addImageCardOverlay overlayRequest : request.getOverlays()) {
				Overlay overlay = Overlay.builder()
					.xPosition(overlayRequest.getPositionOfX())
					.yPosition(overlayRequest.getPositionOfY())
					.width(overlayRequest.getWidth())
					.height(overlayRequest.getHeight())
					.imageCard(savedImageCard)
					.build();

				overlayRepository.save(overlay);
			}
		}

		return savedImageCard.getImageUrl();
	}

	@Transactional
	public List<CardResponse.getStudyCardSetLists> getStudyCardSetLists(String token, String order, String color,
		Integer studyStatus) {
		Long userId = findUserId(token);
		List<StudyCardSet> studyCardSets = cardModuleService.getStudyCardSetsByUser(userId);

		if (studyStatus != null) {
			studyCardSets = studyCardSets.stream()
				.filter(studyCardSet -> studyCardSet.getStudyStatus().getValue() == studyStatus)
				.collect(Collectors.toList());
		}

		if (color != null && !color.isEmpty()) {
			List<String> colorList = Arrays.asList(color.split(","));

			List<String> allowedColors = Arrays.asList("blue", "ocean", "lavender", "mint", "sage", "gray", "orange",
				"coral", "rose", "plum");

			for (String c : colorList) {
				if (!allowedColors.contains(c)) {
					throw new BadRequestException(ErrorResponseStatus.COLOR_REQUEST_ERROR);
				}
			}

			studyCardSets = studyCardSets.stream()
				.filter(studyCardSet -> colorList.contains(studyCardSet.getColor()))
				.collect(Collectors.toList());
		}

		Comparator<StudyCardSet> comparator;
		if (order != null && !order.isEmpty()) {
			switch (order) {
				case "asc":
					comparator = Comparator.comparing(StudyCardSet::getNoteName);
					break;
				case "desc":
					comparator = Comparator.comparing(StudyCardSet::getNoteName).reversed();
					break;
				case "edit-newest":
					comparator = Comparator.comparing(studyCardSet -> studyCardSet.getNote().getEditDate(),
						Comparator.nullsLast(Comparator.reverseOrder()));
					break;
				case "edit-oldest":
					comparator = Comparator.comparing(studyCardSet -> studyCardSet.getNote().getEditDate(),
						Comparator.nullsLast(Comparator.naturalOrder()));
					break;
				default:
					comparator = Comparator.comparing(StudyCardSet::getNoteName);
					break;
			}
			studyCardSets = studyCardSets.stream().sorted(comparator).collect(Collectors.toList());
		}

		return studyCardSets.stream()
			.map(studyCardSet -> CardResponse.getStudyCardSetLists.builder()
				.studyStatus(studyCardSet.getStudyStatus().getValue())
				.noteName(studyCardSet.getNoteName())
				.color(studyCardSet.getColor())
				.folderName(studyCardSet.getFolder().getName())
				.recentStudyDate(studyCardSet.getRecentStudyDate())
				.nextStudyDate(studyCardSet.getNextStudyDate())
				.studyCardSetId(studyCardSet.getId())
				.markStatus(studyCardSet.getNote().getMarkState())
				.build())
			.collect(Collectors.toList());
	}

	/**
	 * 각 카드의 '다음 학습 시간'까지 적게 남은 순서로 전달해야한다.
	 * update date 2025.10.25
	 *
	 * @name getCardLists
	 * @param token
	 * @param studyCardSetId
	 * @param pageNumber
	 * @return Page<Object>
	 */
	@Transactional
	public Page<Object> getCardLists(String token, Long studyCardSetId, int pageNumber) {
		Long userId = findUserId(token);
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		// 1) 일반 카드, 이미지 카드 모두 조회
		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		List<Object> allCards = new ArrayList<>();

		// 2) 난이도가 NONE(0)가 아닌 카드들 또는 countLearn이 0인 카드들만 필터링하여 추가
		allCards.addAll(
			cards.stream()
				.filter(card -> card.getDifficulty() != Difficulty.NONE || card.getCountLearn() == 0)
				.collect(Collectors.toList())
		);

		allCards.addAll(
			imageCards.stream()
				.filter(imageCard -> imageCard.getDifficulty() != Difficulty.NONE || imageCard.getCountLearn() == 0)
				.collect(Collectors.toList())
		);

		log.debug("모든 카드가 결합되고 필터링되었습니다: {}", allCards);

		// 3) 다음 학습 시간 기준으로 오름차순 정렬(남은 시간 적음 -> 많음)
		allCards.sort((a, b) -> {
			LocalDateTime nextA = (a instanceof Card)
					? Optional.ofNullable(((Card)a).getLearnNextTime())
					.map(Timestamp::toLocalDateTime).orElse(LocalDateTime.MAX)
					: Optional.ofNullable(((ImageCard)a).getLearnNextTime())
					.map(Timestamp::toLocalDateTime).orElse(LocalDateTime.MAX);

			LocalDateTime nextB = (b instanceof Card)
					? Optional.ofNullable(((Card)b).getLearnNextTime())
					.map(Timestamp::toLocalDateTime).orElse(LocalDateTime.MAX)
					: Optional.ofNullable(((ImageCard)b).getLearnNextTime())
					.map(Timestamp::toLocalDateTime).orElse(LocalDateTime.MAX);

			int compareTime = nextA.compareTo(nextB);
			if (compareTime != 0) {
				return compareTime; // learnNextTime 기준 우선 정렬
			}

			// ⚙️ learnNextTime이 동일하면 cardId 기준으로 정렬
			Long idA = (a instanceof Card)
					? ((Card)a).getCardId()
					: ((ImageCard)a).getId();
			Long idB = (b instanceof Card)
					? ((Card)b).getCardId()
					: ((ImageCard)b).getId();

			return idA.compareTo(idB);
		});

		int totalCards = allCards.size();
		Pageable pageable = PageRequest.of(pageNumber, 1);

		int start = (int)pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), totalCards);

		List<Object> pagedCards = allCards.subList(start, end);

		Page<Object> cardsPage = new PageImpl<>(pagedCards, pageable, totalCards);

		return cardsPage.map(card -> {
			if (card instanceof Card wordCard) {
				log.debug("WordCard 매핑: {}", wordCard.getCardId());
				return mapToWordCardResponse(wordCard, studyCardSet);
			} else if (card instanceof ImageCard imageCard) {
				log.debug("ImageCard 매핑: {}", imageCard.getId());
				return mapToImageCardResponse(imageCard, studyCardSet);
			} else {
				log.error("예상치 못한 카드 유형이 발견되었습니다: {}", card.getClass().getName());
				throw new IllegalStateException("예상치 못한 카드 유형입니다");
			}
		});
	}

	private CardResponse.getCardLists mapToWordCardResponse(Card card, StudyCardSet studyCardSet) {
		CardResponse.getCardLists getCardLists;
		if (card.getCardType() == CardType.BLANK) {
			getCardLists = CardResponse.getCardLists.builder()
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.answer(card.getAnswer())
				.cardId(card.getCardId())
				.noteId(studyCardSet.getNote().getNoteId())
				.folderId(studyCardSet.getFolder().getFolderId())
				.cardType("blank")
				.build();
		} else if (card.getCardType() == CardType.WORD) {
			getCardLists = CardResponse.getCardLists.builder()
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.answer(card.getAnswer())
				.cardId(card.getCardId())
				.noteId(studyCardSet.getNote().getNoteId())
				.folderId(studyCardSet.getFolder().getFolderId())
				.cardType("word")
				.build();
		} else { // 멀티 카드
			String multiAnswer = card.getAnswer();

			List<String> answersList = Arrays.stream(multiAnswer.split(",\\s*")).toList();

			getCardLists = CardResponse.getCardLists.builder()
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.answer(card.getAnswer())
				.cardId(card.getCardId())
				.noteId(studyCardSet.getNote().getNoteId())
				.folderId(studyCardSet.getFolder().getFolderId())
				.cardType("multi")
				.multiAnswer(answersList)
				.build();
		}

		return getCardLists;
	}

	private CardResponse.getImageCard mapToImageCardResponse(ImageCard imageCard, StudyCardSet studyCardSet) {
		return CardResponse.getImageCard.builder()
			.imgUrl(imageCard.getImageUrl())
			.baseImageWidth(imageCard.getWidth())
			.baseImageHeight(imageCard.getHeight())
			.overlays(convertOverlays(imageCard.getOverlays()))
			.imageCardId(imageCard.getId())
			.noteId(studyCardSet.getNote().getNoteId())
			.folderId(studyCardSet.getFolder().getFolderId())
			.cardType("image")
			.build();
	}

	private List<CardRequest.addImageCardOverlay> convertOverlays(List<Overlay> overlays) {
		List<CardRequest.addImageCardOverlay> overlayDtos = new ArrayList<>();
		for (Overlay overlay : overlays) {
			overlayDtos.add(CardRequest.addImageCardOverlay.builder()
				.positionOfX(overlay.getXPosition())
				.positionOfY(overlay.getYPosition())
				.width(overlay.getWidth())
				.height(overlay.getHeight())
				.build());
		}
		return overlayDtos;
	}

	/**
	 * 난이도 선택 시 다음 학습 시간, 난이도, 학습 횟수 상태 업데이트
	 * update date 2025.10.25
	 *
	 * @name updateCardDifficulty
	 * @param token
	 * @param request
	 */
	@Transactional
	public void updateCardDifficulty(String token, CardRequest.difficulty request) {
		Long userId = findUserId(token);

		if (request.getDifficulty() > 4 || request.getDifficulty() < 1) {
			throw new BadRequestException(NOT_EXIST_DIFFICULTY_CODE);
		}

		// 카드 난이도 선택
		if (request.getCardType() == 0 || request.getCardType() == 1 || request.getCardType() == 2) {
			Card card = cardModuleService.getCardById(request.getCardId());
			card.setDifficulty(request.getDifficulty());

			Timestamp next = calculateNextStudyTime(card);
			cardModuleService.updateWordCardDifficulty(card);
		} else {
			ImageCard imageCard = cardModuleService.getImageCardById(request.getCardId());
			imageCard.setDifficulty(request.getDifficulty());
			Timestamp next = calculateNextStudyTime(imageCard);
			cardModuleService.updateImageCardDifficulty(imageCard);
		}

		// 카드 개별로 학습한 후에 분석학습이 완료됨
		completeStudy(token, request.getCardId(), request.getCardType());
	}

	public CardResponse.cardStudyGraph viewStudyCardGraph(String token, Long studyCardSetId) {
		Long userId = findUserId(token);

		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		int easyCardsCount = 0;
		int normalCardsCount = 0;
		int hardCardsCount = 0;
		int expertCardsCount = 0;

		int totalCards = cards.size() + imageCards.size();

		for (Card card : cards) {
			switch (card.getDifficulty()) {
				case NONE:
					continue;
				case EASY:
					easyCardsCount++;
					break;
				case NORMAL:
					normalCardsCount++;
					break;
				case HARD:
					hardCardsCount++;
					break;
				case EXPERT:
					expertCardsCount++;
					break;
			}
		}

		for (ImageCard card : imageCards) {
			switch (card.getDifficulty()) {
				case NONE:
					continue;
				case EASY:
					easyCardsCount++;
					break;
				case NORMAL:
					normalCardsCount++;
					break;
				case HARD:
					hardCardsCount++;
					break;
				case EXPERT:
					expertCardsCount++;
					break;
			}
		}

		int easyCardsPercent = (easyCardsCount * 100) / totalCards;
		int normalCardsPercent = (normalCardsCount * 100) / totalCards;
		int hardCardsPercent = (hardCardsCount * 100) / totalCards;
		int expertCardsPercent = (expertCardsCount * 100) / totalCards;

		return CardResponse.cardStudyGraph.builder()
			.easyCardsNumber(easyCardsCount)
			.normalCardsNumber(normalCardsCount)
			.hardCardsNumber(hardCardsCount)
			.expertCardsNumber(expertCardsCount)
			.easyCardsPercent(easyCardsPercent)
			.normalCardsPercent(normalCardsPercent)
			.hardCardsPercent(hardCardsPercent)
			.expertCardsPercent(expertCardsPercent)
			.build();
	}

	@Transactional
	public Page<CardResponse.getStudyLog> viewStudyLog(String token, Long studyCardSetId, int page, int size) {
		Long userId = findUserId(token);

		Pageable pageable = PageRequest.of(page, size);

		StudyCardSet studyCardSet = studyCardSetRepository.findById(studyCardSetId)
			.orElseThrow(() -> new DatabaseException(NOT_FOUND_ERROR));

		Page<StudyLog> studyLogsPage = studyLogRepository.findByStudyCardSet(studyCardSet, pageable);

		return studyLogsPage.map(studyLog -> CardResponse.getStudyLog.builder()
			.cardNumber(studyLog.getStudyCardNumber())
			.studyDate(studyLog.getStudyDate())
			.build());
	}

	/**
	 * 실제 난이도에 따른 다음 학습 시간 계산하여 DB 업데이트
	 * update date 2025.10.25
	 *
	 * @name calculateNextStudyTime(Card)
	 * @param card
	 * @return Timestamp
	 */
	public Timestamp calculateNextStudyTime(Card card) {
		LocalDateTime currentTime = LocalDateTime.now();
		long baseInterval; // 기본 학습 간격 (분 단위로 계산)
		double increaseFactor; // 증가 비율
		long nextInterval; // 계산된 다음 학습 간격 (분)

		switch (card.getDifficulty()) {
			case EXPERT: // 난이도 1
				baseInterval = 0; // 즉시 학습
				increaseFactor = 0.0;
				break;
			case HARD: // 난이도 2
				baseInterval = 10; // 10분
				increaseFactor = 1.0;
				break;
			case NORMAL: // 난이도 3
				baseInterval = 30; // 30분
				increaseFactor = 1.5;
				break;
			case EASY: // 난이도 4
				baseInterval = 24 * 60; // 24시간 (1일)
				increaseFactor = 2.0;
				break;
			default:
				baseInterval = 0; // 재학습 or 새로 만든 카드라면, 즉시학습으로 지정되어 baseInterval 0, increaseFactor = 0.0
				increaseFactor = 0.0;
		}

		log.debug("Card {} - Difficulty: {}", card.getCardId(), card.getDifficulty());
		log.debug("Card {} - Base Interval (minutes): {}", card.getCardId(), baseInterval);
		log.debug("Card {} - Increase Factor: {}", card.getCardId(), increaseFactor);

		// 1. 학습 간격 계산
		if (card.getCountLearn() == 0L) {
			// 첫 학습인 경우
			nextInterval = baseInterval;
			log.debug("Card {} - First Learning: using base interval {}", card.getCardId(), baseInterval);
		} else {
			// 누적 간격 증가
			long prevInterval = baseInterval;
			if (card.getLearnLastTime() != null && card.getLearnNextTime() != null) {
				try {
					prevInterval = ChronoUnit.MINUTES.between(
							card.getLearnLastTime().toLocalDateTime(),
							card.getLearnNextTime().toLocalDateTime()
					);
					if (prevInterval <= 0) {
						// 만약 prevInterval이 0 이하라면 baseInterval로 보정
						prevInterval = baseInterval > 0 ? baseInterval : 1;
					}
				} catch (Exception e) {
					log.warn("prevInterval 계산 실패, fallback to baseInterval", e);
					prevInterval = baseInterval > 0 ? baseInterval : 1;
				}
			}
			nextInterval = (long) Math.round(prevInterval * increaseFactor);
			log.debug("countLearn>0 -> prevInterval={} * increaseFactor={} => nextInterval={}",
					prevInterval, increaseFactor, nextInterval);
		}

		// 2. EXPERT 즉시학습
		LocalDateTime nextStudyTime;
		if (card.getDifficulty() == Difficulty.EXPERT) {
			nextStudyTime = currentTime;
			nextInterval = 0;
		} else {
			nextStudyTime = currentTime.plusMinutes(nextInterval);
		}

		card.setLearnLastTime(Timestamp.valueOf(currentTime));
		card.setLearnNextTime(Timestamp.valueOf(nextStudyTime));

		log.debug("Card {} - Current Interval (minutes): {}", card.getCardId(), nextInterval);
		log.debug("Card {} - Next Study Time: {}", card.getCardId(), card.getLearnNextTime());

		return card.getLearnNextTime();
	}

	/**
	 * 실제 난이도에 따른 다음 학습 시간 계산하여 DB 업데이트
	 * update date 2025.10.25
	 *
	 * @name calculateNextStudyTime(ImageCard)
	 * @param imageCard
	 * @return Timestamp
	 */
	public Timestamp calculateNextStudyTime(ImageCard imageCard) {
		LocalDateTime currentTime = LocalDateTime.now();
		long baseInterval; // 기본 학습 간격 (분 단위로 계산)
		double increaseFactor; // 증가 비율
		long nextInterval; // 다음 학습 간격

		// 난이도가 NONE인 경우, 학습을 하지 않으므로 null 반환
		if (imageCard.getDifficulty() == Difficulty.NONE) {
			log.debug("ImageCard {} has difficulty NONE, skipping calculation.", imageCard.getId());
			return null;
		}

		switch (imageCard.getDifficulty()) {
			case EXPERT: // 난이도 1
				baseInterval = 0; // 즉시 학습
				increaseFactor = 0;
				break;
			case HARD: // 난이도 2
				baseInterval = 10; // 10분
				increaseFactor = 1;
				break;
			case NORMAL: // 난이도 3
				baseInterval = 30; // 30분
				increaseFactor = 1.5;
				break;
			case EASY: // 난이도 4
				baseInterval = 24 * 60; // 24시간 (1일)
				increaseFactor = 2;
				break;
			default:
				baseInterval = 0; // 기본적으로 현재 난이도가 없는 경우(새로 만든 경우 or 학습횟수가 0으로 돌아간 경우)
				increaseFactor = 1;
		}

		log.debug("ImageCard {} - Difficulty: {}", imageCard.getId(), imageCard.getDifficulty());
		log.debug("ImageCard {} - Base Interval (minutes): {}", imageCard.getId(), baseInterval);
		log.debug("ImageCard {} - Increase Factor: {}", imageCard.getId(), increaseFactor);

		// 1. 학습 간격 계산
		if (imageCard.getCountLearn() == 0) {
			// 첫 학습인 경우
			nextInterval = baseInterval;
			log.debug("ImageCard {} - First Learning: using base interval {}", imageCard.getId(), baseInterval);
		} else {
			// 누적 간격 증가
			long prevInterval = 0;
			if (imageCard.getLearnLastTime() != null && imageCard.getLearnNextTime() != null) {
				try {
					prevInterval = ChronoUnit.MINUTES.between(
							imageCard.getLearnLastTime().toLocalDateTime(),
							imageCard.getLearnNextTime().toLocalDateTime()
					);
					if (prevInterval <= 0) {
						// 만약 prevInterval이 0 이하라면 baseInterval로 보정
						prevInterval = baseInterval > 0 ? baseInterval : 1;
					}
				} catch (Exception e) {
					log.warn("prevInterval 계산 실패, fallback to baseInterval", e);
					prevInterval = baseInterval > 0 ? baseInterval : 1;
				}
			}
			nextInterval = (long) Math.round(prevInterval * increaseFactor);
			log.debug("countLearn>0 -> prevInterval={} * increaseFactor={} => nextInterval={}",
					prevInterval, increaseFactor, nextInterval);
		}

		// 2. EXPERT 즉시학습
		LocalDateTime nextStudyTime;
		if (imageCard.getDifficulty() == Difficulty.EXPERT) {
			nextStudyTime = currentTime;
			nextInterval = 0;
		} else {
			nextStudyTime = currentTime.plusMinutes(nextInterval);
		}

		imageCard.setLearnLastTime(Timestamp.valueOf(currentTime));
		imageCard.setLearnNextTime(Timestamp.valueOf(nextStudyTime));
		imageCard.setCountLearn(imageCard.getCountLearn() + 1);

		log.debug("ImageCard {} - Current Interval (minutes): {}", imageCard.getId(), nextInterval);
		log.debug("ImageCard {} - Next Study Time: {}", imageCard.getId(), imageCard.getLearnNextTime());

		return imageCard.getLearnNextTime();
	}


	public void addCardToNote(Card card, Note note) {
		Card newCard = Card.builder()
			.note(note)
			.contentsFront(card.getContentsFront())
			.contentsBack(card.getContentsBack())
			.countLearn(0L)
			.build();

		cardModuleService.saveCard(newCard);
	}

	/**
	 * 난이도 전달 시 분석학습 전달 내부 메서드(다음 학습 시간 저장, 학습 횟수 증가)
	 * update date 2025.10.25
	 *
	 * @name completeStudy
	 * @param token
	 * @param cardId
	 * @param cardType
	 */
	// NOTE : findByUserAndCard, findByUserAndImageCard 유의
	private void completeStudy(String token, Long cardId, int cardType) {
		Long userId = findUserId(token);
		int difficulty = cardModuleService.getCardDifficulty(cardId, cardType);
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		if(cardType == 0) {
			Card card = cardModuleService.getCardById(cardId);
			card.setDifficulty(difficulty);
			card.setCountLearn(card.getCountLearn() == null ? 1 : card.getCountLearn() + 1);

			Timestamp nextStudyTime = calculateNextStudyTime(card);
			//card.setLearnNextTime(nextStudyTime);
			cardModuleService.saveCard(card);

			// StudyLog
			studyLogRepository.save(StudyLog.builder()
					.user(card.getStudyCardSet().getUser())
					.card(card)
					.difficulty(difficulty)
					.studyDate(now)
					.build());

			// StudyHistory 갱신
			StudyHistory history = studyHistoryRepository.findByUserAndCard(card.getStudyCardSet().getUser(), card)
					.orElse(StudyHistory.builder()
							.user(card.getStudyCardSet().getUser())
							.card(card)
							.studyDate(LocalDateTime.now())
							.totalLearnCount(0)
							.build());

			history.setTotalLearnCount(history.getTotalLearnCount() + 1);
			studyHistoryRepository.save(history);

			// StudyCardSet 갱신
			StudyCardSet scs = card.getStudyCardSet();
			if (scs != null) {
				// recentStudyDate : 마지막 학습시각 (card.learnLastTime)
				scs.setRecentStudyDate(card.getLearnLastTime() == null ? null :
						card.getLearnLastTime().toLocalDateTime());
				// nextStudyDate : 가장 최근에 학습한 카드의 nextStudyTime (또는 전체 세트의 다음 예정시간을 계산하려면 별도 로직 필요)
				scs.setNextStudyDate(card.getLearnNextTime() == null ? null :
						card.getLearnNextTime().toLocalDateTime());
				// completedCardsCount 증가 (null 안전 처리)
				Integer prev = scs.getCompletedCardsCount();
				scs.setCompletedCardsCount(prev == null ? 1 : prev + 1);

				studyCardSetRepository.save(scs); // 반드시 저장
			}

		} else {
			ImageCard imageCard = cardModuleService.getImageCardById(cardId);
			imageCard.setDifficulty(difficulty);
			imageCard.setCountLearn(imageCard.getCountLearn() == null ? 1 : imageCard.getCountLearn() + 1);
			//imageCard.setLearnLastTime(Timestamp.valueOf(now));

			Timestamp nextStudyTime = calculateNextStudyTime(imageCard);
			//imageCard.setLearnNextTime(nextStudyTime);
			cardModuleService.saveImageCard(imageCard);

			studyLogRepository.save(StudyLog.builder()
					.user(imageCard.getStudyCardSet().getUser())
					.imageCard(imageCard)
					.difficulty(difficulty)
					.studyDate(now)
					.build());

			StudyHistory history = studyHistoryRepository.findByUserAndImageCard(
							imageCard.getStudyCardSet().getUser(), imageCard)
					.orElse(StudyHistory.builder()
							.user(imageCard.getStudyCardSet().getUser())
							.imageCard(imageCard)
							.studyDate(LocalDateTime.now())
							.totalLearnCount(0)
							.build());

			history.setTotalLearnCount(history.getTotalLearnCount() + 1);
			studyHistoryRepository.save(history);

			// StudyCardSet 갱신
			StudyCardSet scs = imageCard.getStudyCardSet();
			if (scs != null) {
				scs.setRecentStudyDate(imageCard.getLearnLastTime() == null ? null :
						imageCard.getLearnLastTime().toLocalDateTime());
				scs.setNextStudyDate(imageCard.getLearnNextTime() == null ? null :
						imageCard.getLearnNextTime().toLocalDateTime());
				Integer prev = scs.getCompletedCardsCount();
				scs.setCompletedCardsCount(prev == null ? 1 : prev + 1);

				studyCardSetRepository.save(scs);
			}
		}
	}

	public CardResponse.weeklyResultDTO getCardByWeek(String token) {
		Long userId = findUserId(token);
		User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException(INVALID_USERID));

		LocalDate today = LocalDate.now();
		LocalDate startOfWeek = today.with(DayOfWeek.MONDAY); //DayOfWeek.of(1)
		LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

		LocalDate startOfLastWeek = startOfWeek.minusWeeks(1);
		LocalDate endOfLastWeek = endOfWeek.minusWeeks(1);

		List<Card> thisWeekCards = cardRepository.findCardsByUserAndLearnLastTimeBetween(user,
			startOfWeek.atStartOfDay(), endOfWeek.atTime(LocalTime.MAX));
		List<Card> lastWeekCards = cardRepository.findCardsByUserAndLearnLastTimeBetween(user,
			startOfLastWeek.atStartOfDay(), endOfLastWeek.atTime(LocalTime.MAX));

		Map<Integer, Long> dailyThisWeekStudy = calculateDailyStudyCount(thisWeekCards);
		Map<Integer, Long> dailyLastWeekStudy = calculateDailyStudyCount(lastWeekCards);

		Map<Integer, Long> weekStudyResult = initializeWeekStudyResult(dailyThisWeekStudy);
		Map<Integer, Long> lastWeekStudyResult = initializeWeekStudyResult(dailyLastWeekStudy);

		// 이번 주 총 학습 카드 수 계산
		long totalThisWeekStudy = weekStudyResult.values().stream().mapToLong(Long::longValue).sum();

		return CardResponse.weeklyResultDTO.builder()
			.thisWeekCardCount(totalThisWeekStudy)
			.dayOfThisWeekCard(weekStudyResult)
			.dayOfLastWeekCard(lastWeekStudyResult)
			.build();
	}

	private Map<Integer, Long> calculateDailyStudyCount(List<Card> cards) {
		return cards.stream()
			.collect(Collectors.groupingBy(card -> card.getLearnLastTime().toLocalDateTime().getDayOfWeek().getValue(),
				Collectors.collectingAndThen(
					Collectors.toMap(card -> card.getStudyCardSet(), card -> card.getLearnLastTime(),
						(time1, time2) -> time1.before(time2) ? time1 : time2), map -> (long)map.size())));
	}

	public Map<Integer, Long> initializeWeekStudyResult(Map<Integer, Long> dailyStudyCount) {
		Map<Integer, Long> weekResult = new HashMap<>();

		for (int i = 1; i <= 7; i++) {
			weekResult.put(i, dailyStudyCount.getOrDefault(i, 0L));
		}
		return weekResult;
	}

	public List<CardResponse.getExpectedCardSetListDTO> getStudyCardSetsForQuickLearning(String token) {
		Long userId = findUserId(token);
		List<StudyCardSet> studyCardSets = studyCardSetRepository.findByUserOrderByNextStudyDateAsc(userId);

		studyCardSets = studyCardSets.stream()
				.filter(set -> set.getProgressRate() < 1.0)
				.limit(3)
				.collect(Collectors.toList());

		return studyCardSets.stream()
				.map(set -> CardResponse.getExpectedCardSetListDTO.builder()
						.studyCardSetId(set.getId())
						.folderName(set.getFolder() != null ? set.getFolder().getName() : null)
						.noteName(set.getNoteName())
						.color(set.getColor())
						.cardsDueForStudy(set.getCardsDueForStudy())
						.completedCardsCount(set.getCompletedCardsCount())
						.progressRate(set.getProgressRate())
						.recentStudyDate(set.getRecentStudyDate())
						.nextStudyDate(set.getNextStudyDate())
						.build())
				.collect(Collectors.toList());
	}


	/**
	 * 예측 학습 시간 반환
	 * update date 2025.10.25
	 *
	 * @name getExpectedNextStudyTimes
	 * @param cardId
	 * @param cardType
	 * @return Map<String, LocalDateTime>
	 */
	// NOTE : 난이도 선택에서 학습 시간 계산하여 반환
	public Map<String, LocalDateTime> getExpectedNextStudyTimes(Long cardId, int cardType) {
		Map<String, LocalDateTime> result = new LinkedHashMap<>();

		int countLearn = cardModuleService.getCardCountLearn(cardId, cardType);
		LocalDateTime currentTime = LocalDateTime.now();

		for (Difficulty diff : Difficulty.values()) {
			if (diff == Difficulty.NONE) continue;

			LocalDateTime expectedTime = calculateExpectedNextStudyTime(diff, countLearn, currentTime);
			result.put(diff.name(), expectedTime);
		}

		return result;
	}

	/**
	 * 학습 난이도별 다음 학습 시간 계산 (예측용)
	 * update date 2025.10.25
	 *
	 * @name calculateExpectedNextStudyTime
	 * @param difficulty 난이도
	 * @param countLearn 현재 학습 횟수
	 * @param currentTime 현재 시각 (기준 시각)
	 * @return 예측되는 다음 학습 시각(LocalDateTime)
	 */
	private LocalDateTime calculateExpectedNextStudyTime(Difficulty difficulty, int countLearn, LocalDateTime currentTime) {
		long baseInterval;
		double increaseFactor;

		switch (difficulty) {
			case EXPERT: // 난이도 1
				baseInterval = 0; // 즉시 학습
				increaseFactor = 0.0;
				break;
			case HARD: // 난이도 2
				baseInterval = 10; // 10분
				increaseFactor = 1.0;
				break;
			case NORMAL: // 난이도 3
				baseInterval = 30; // 30분
				increaseFactor = 1.5;
				break;
			case EASY: // 난이도 4
				baseInterval = 24 * 60; // 24시간 (1일)
				increaseFactor = 2.0;
				break;
			default:
				return null;
		}


		// 학습 횟수에 따른 누적 간격 계산
		// countLearn = 0 → 첫 학습 = baseInterval
		// countLearn ≥ 1 → baseInterval * (increaseFactor ^ countLearn)
		double totalMinutes = baseInterval * Math.pow(increaseFactor, countLearn > 0 ? countLearn : 1);

		// EXPERT의 경우 즉시 복습 처리
		if (difficulty == Difficulty.EXPERT) {
			return currentTime; // 즉시 복습 (지금)
		}

		// 계산된 간격을 현재 시각에 더함
		LocalDateTime nextTime = currentTime.plusMinutes((long) totalMinutes);

		// 한국 시간대 명시 (서버가 UTC일 수 있으므로)
		return nextTime.atZone(ZoneId.systemDefault())
				.withZoneSameInstant(ZoneId.of("Asia/Seoul"))
				.toLocalDateTime();
	}

	/**
	 * 365일치 DTO 리스트 반환
	 * update date 2025.11.2
	 *
	 * @name getCardByYear
	 * @param token
	 * @param year
	 * @return CardResponse.AnnualResultDTO
	 */
	public CardResponse.AnnualResultDTO getCardByYear(String token, int year) {
		Long userId = findUserId(token);

		LocalDate startOfYear = LocalDate.of(year, 1, 1);
		LocalDate endOfYear = LocalDate.of(year, 12, 31);

		LocalDateTime start = startOfYear.atStartOfDay();
		LocalDateTime end = endOfYear.atTime(LocalTime.MAX);

		long t0 = System.nanoTime();
		List<Object[]> rows = studyHistoryRepository.findDailyCountWithWeekMaxNative(userId, start, end);
		long t1 = System.nanoTime();

		// Map<날짜, [count, weekMax]>
		Map<LocalDate, long[]> daily = new HashMap<>();
		for (Object[] row : rows) {
			java.sql.Date sqlDate = (java.sql.Date) row[0];
			Number countNum = (Number) row[1];
			Number weekMaxNum = (Number) row[2];
			LocalDate date = sqlDate.toLocalDate();
			long count = countNum == null ? 0 : countNum.longValue();
			long weekMax = weekMaxNum == null ? 0 : weekMaxNum.longValue();

			daily.put(date, new long[]{count, weekMax});
		}

		// 1년치 데이터 순회
		List<CardResponse.DailyContribution> contributions = new ArrayList<>();
		int maxStreak = 0, currentStreak = 0;

		LocalDate currentDate = startOfYear;
		while (!currentDate.isAfter(endOfYear)) {
			long count = 0, weekMax = 0;
			if (daily.containsKey(currentDate)) {
				count = daily.get(currentDate)[0];
				weekMax = daily.get(currentDate)[1];
			}

			// 비율 측정 : 일주일 단위 내에서 비교하여 비율 측정
			String color;
			if (count == 0 || weekMax == 0) {
				color = "1";
				currentStreak = 0;
			} else {
				double ratio = (double) count / (double) weekMax;
				if (ratio >= 0.75) color = "4";
				else if (ratio >= 0.5) color = "3";
				else if (ratio >= 0.25) color = "2";
				else color = "1";
				currentStreak++;
			}

			maxStreak = Math.max(maxStreak, currentStreak);
			contributions.add(new CardResponse.DailyContribution(currentDate, count, color));

			currentDate = currentDate.plusDays(1);
		}

		long t2 = System.nanoTime();

		return new CardResponse.AnnualResultDTO(contributions, maxStreak);

	}
}

