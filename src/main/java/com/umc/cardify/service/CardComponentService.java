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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.umc.cardify.auth.jwt.JwtTokenProvider;
import com.umc.cardify.domain.enums.AuthProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.Overlay;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.StudyLog;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.CardType;
import com.umc.cardify.domain.enums.Difficulty;
import com.umc.cardify.domain.enums.StudyStatus;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.dto.card.CardResponse;
import com.umc.cardify.repository.CardRepository;
import com.umc.cardify.repository.ImageCardRepository;
import com.umc.cardify.repository.OverlayRepository;
import com.umc.cardify.repository.StudyCardSetRepository;
import com.umc.cardify.repository.StudyLogRepository;
import com.umc.cardify.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardComponentService {
	private final CardModuleService cardModuleService;
	private final S3Service s3Service;

	private final ImageCardRepository imageCardRepository;
	private final OverlayRepository overlayRepository;
	private final StudyCardSetRepository studyCardSetRepository;
	private final StudyLogRepository studyLogRepository;
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
	public void reStudy(String token, Long studyCardSetId) {
		Long userId = findUserId(token);

		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);
		studyCardSet.setStudyStatus(StudyStatus.BEFORE_STUDY);
		studyCardSet.setNextStudyDate(null);
		studyCardSet.setRecentStudyDate(null);
		studyCardSetRepository.save(studyCardSet);

		studyLogRepository.deleteAllByStudyCardSet(studyCardSet);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		for (Card card : cards) {
			card.setCountLearn(0L);
			card.setDifficulty(0);
			card.setLearnLastTime(null);
			card.setLearnNextTime(null);
			cardModuleService.saveCard(card);
		}

		for (ImageCard imageCard : imageCards) {
			imageCard.setCountLearn(0L);
			imageCard.setDifficulty(0);
			imageCard.setLearnLastTime(null);
			imageCard.setLearnNextTime(null);
			cardModuleService.saveImageCard(imageCard);
		}
	}

	@Transactional
	public void deleteStudyCardSet(String token, Long studyCardSetId) {
		Long userId = findUserId(token);

		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);
		cardModuleService.deleteCardSet(studyCardSetId);
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
		}), imageCards.stream().map(imageCard -> {
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
	public String addImageCard(String token, MultipartFile image, CardRequest.addImageCard request, Note note) {
		Long userId = findUserId(token);
		String imgUrl = s3Service.upload(image, "imageCards");

		ImageCard imageCard = ImageCard.builder()
			.imageUrl(imgUrl)
			.height(request.getBaseImageHeight())
			.width(request.getBaseImageWidth())
			.build();

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

	@Transactional
	public Page<Object> getCardLists(String token, Long studyCardSetId, int pageNumber) {
		Long userId = findUserId(token);
		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		List<Object> allCards = new ArrayList<>();

		// 난이도가 PASS(4)가 아닌 카드들 또는 countLearn이 0인 카드들만 필터링하여 추가
		allCards.addAll(
			cards.stream()
				.filter(card -> card.getDifficulty() != Difficulty.PASS || card.getCountLearn() == 0)
				.collect(Collectors.toList())
		);

		allCards.addAll(
			imageCards.stream()
				.filter(imageCard -> imageCard.getDifficulty() != Difficulty.PASS || imageCard.getCountLearn() == 0)
				.collect(Collectors.toList())
		);

		log.debug("모든 카드가 결합되고 필터링되었습니다: {}", allCards);

		// 생성된 날짜를 기준으로 카드들을 정렬
		allCards.sort((a, b) -> {
			LocalDateTime createdA = (a instanceof Card) ? ((Card)a).getCreatedAt() : ((ImageCard)a).getCreatedAt();
			LocalDateTime createdB = (b instanceof Card) ? ((Card)b).getCreatedAt() : ((ImageCard)b).getCreatedAt();
			return createdA.compareTo(createdB);
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

	public void updateCardDifficulty(String token, CardRequest.difficulty request) {
		Long userId = findUserId(token);

		if (request.getDifficulty() > 4 || request.getDifficulty() < 1) {
			throw new BadRequestException(NOT_EXIST_DIFFICULTY_CODE);
		}

		if (request.getCardType() == 0) {
			Card card = cardModuleService.getCardById(request.getCardId());
			card.setDifficulty(request.getDifficulty());
			cardModuleService.updateWordCardDifficulty(card);
		} else {
			ImageCard imageCard = cardModuleService.getImageCardById(request.getCardId());
			imageCard.setDifficulty(request.getDifficulty());
			cardModuleService.updateImageCardDifficulty(imageCard);
		}

	}

	public CardResponse.cardStudyGraph viewStudyCardGraph(String token, Long studyCardSetId) {
		Long userId = findUserId(token);

		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		int easyCardsCount = 0;
		int normalCardsCount = 0;
		int hardCardsCount = 0;
		int passCardsCount = 0;

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
				case PASS:
					passCardsCount++;
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
				case PASS:
					passCardsCount++;
					break;
			}
		}

		int easyCardsPercent = (easyCardsCount * 100) / totalCards;
		int normalCardsPercent = (normalCardsCount * 100) / totalCards;
		int hardCardsPercent = (hardCardsCount * 100) / totalCards;
		int passCardsPercent = (passCardsCount * 100) / totalCards;

		return CardResponse.cardStudyGraph.builder()
			.easyCardsNumber(easyCardsCount)
			.normalCardsNumber(normalCardsCount)
			.hardCardsNumber(hardCardsCount)
			.passCardsNumber(passCardsCount)
			.easyCardsPercent(easyCardsPercent)
			.normalCardsPercent(normalCardsPercent)
			.hardCardsPercent(hardCardsPercent)
			.passCardsPercent(passCardsPercent)
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

	public Timestamp calculateNextStudyTime(Card card) {
		LocalDateTime currentTime = LocalDateTime.now();
		long baseInterval; // 기본 학습 간격 (분 단위로 계산)
		double increaseFactor; // 증가 비율
		long currentInterval; // 현재 학습 간격

		// 난이도가 NONE인 경우, 학습을 하지 않으므로 null 반환
		if (card.getDifficulty() == Difficulty.NONE) {
			log.debug("Card {} has difficulty NONE, skipping calculation.", card.getCardId());
			return null;
		}

		switch (card.getDifficulty()) {
			case HARD: // 난이도 1
				baseInterval = 30; // 30분 (0.5시간)
				increaseFactor = 1;
				break;
			case NORMAL: // 난이도 2
				baseInterval = 60; // 1시간
				increaseFactor = 1.5;
				break;
			case EASY: // 난이도 3
				baseInterval = 48 * 60; // 48시간 (2일)
				increaseFactor = 2;
				break;
			default:
				baseInterval = 60; // 기본적으로 1시간으로 설정
				increaseFactor = 1;
		}

		log.debug("Card {} - Difficulty: {}", card.getCardId(), card.getDifficulty());
		log.debug("Card {} - Base Interval (minutes): {}", card.getCardId(), baseInterval);
		log.debug("Card {} - Increase Factor: {}", card.getCardId(), increaseFactor);

		if (card.getCountLearn() == 0) {
			// 첫 학습인 경우
			currentInterval = baseInterval;
			card.setLearnLastTime(Timestamp.valueOf(currentTime)); // 학습 시점을 기록
			card.setLearnNextTime(Timestamp.valueOf(currentTime.plusMinutes(currentInterval)));
		} else {
			// 이미 학습된 카드인 경우
			long timeSinceLastLearn = card.getLearnLastTime().toLocalDateTime().until(currentTime, ChronoUnit.MINUTES);
			log.debug("Card {} - Time Since Last Learn (minutes): {}", card.getCardId(), timeSinceLastLearn);
			currentInterval = (long)(baseInterval * increaseFactor); // 기본 간격에 증가 비율을 적용
			card.setLearnNextTime(Timestamp.valueOf(currentTime.plusMinutes(currentInterval)));
		}

		card.setCountLearn(card.getCountLearn() + 1); // 학습 횟수를 증가시킴

		log.debug("Card {} - Current Interval (minutes): {}", card.getCardId(), currentInterval);
		log.debug("Card {} - Next Study Time: {}", card.getCardId(), card.getLearnNextTime());

		return card.getLearnNextTime();
	}

	public Timestamp calculateNextStudyTime(ImageCard imageCard) {
		LocalDateTime currentTime = LocalDateTime.now();
		long baseInterval; // 기본 학습 간격 (분 단위로 계산)
		double increaseFactor; // 증가 비율
		long currentInterval; // 현재 학습 간격

		// 난이도가 NONE인 경우, 학습을 하지 않으므로 null 반환
		if (imageCard.getDifficulty() == Difficulty.NONE) {
			log.debug("ImageCard {} has difficulty NONE, skipping calculation.", imageCard.getId());
			return null;
		}

		switch (imageCard.getDifficulty()) {
			case HARD: // 난이도 1
				baseInterval = 30; // 30분 (0.5시간)
				increaseFactor = 1;
				break;
			case NORMAL: // 난이도 2
				baseInterval = 60; // 1시간
				increaseFactor = 1.5;
				break;
			case EASY: // 난이도 3
				baseInterval = 48 * 60; // 48시간 (2일)
				increaseFactor = 2;
				break;
			default:
				baseInterval = 60; // 기본적으로 1시간으로 설정
				increaseFactor = 1;
		}

		log.debug("ImageCard {} - Difficulty: {}", imageCard.getId(), imageCard.getDifficulty());
		log.debug("ImageCard {} - Base Interval (minutes): {}", imageCard.getId(), baseInterval);
		log.debug("ImageCard {} - Increase Factor: {}", imageCard.getId(), increaseFactor);

		if (imageCard.getCountLearn() == 0) {
			// 첫 학습인 경우
			currentInterval = baseInterval;
			imageCard.setLearnLastTime(Timestamp.valueOf(currentTime)); // 학습 시점을 기록
			imageCard.setLearnNextTime(Timestamp.valueOf(currentTime.plusMinutes(currentInterval)));
		} else {
			// 이미 학습된 카드인 경우
			long timeSinceLastLearn = imageCard.getLearnLastTime()
				.toLocalDateTime()
				.until(currentTime, ChronoUnit.MINUTES);
			log.debug("ImageCard {} - Time Since Last Learn (minutes): {}", imageCard.getId(), timeSinceLastLearn);
			currentInterval = (long)(baseInterval * increaseFactor); // 기본 간격에 증가 비율을 적용
			imageCard.setLearnNextTime(Timestamp.valueOf(currentTime.plusMinutes(currentInterval)));
		}

		imageCard.setCountLearn(imageCard.getCountLearn() + 1); // 학습 횟수를 증가시킴

		log.debug("ImageCard {} - Current Interval (minutes): {}", imageCard.getId(), currentInterval);
		log.debug("ImageCard {} - Next Study Time: {}", imageCard.getId(), imageCard.getLearnNextTime());

		return imageCard.getLearnNextTime();
	}

	@Transactional
	public void completeStudy(String token, Long studyCardSetId) {
		Long userId = findUserId(token);

		StudyCardSet studyCardSet = cardModuleService.getStudyCardSetById(studyCardSetId);

		List<Card> cards = cardModuleService.getCardsByStudyCardSet(studyCardSet);
		List<ImageCard> imageCards = cardModuleService.getImageCardsByStudyCardSet(studyCardSet);

		int remainingCardsCount = cards.size() + imageCards.size();

		int compltedCount = 0;
		for (Card card : cards) {
			if (card.getDifficulty() == Difficulty.PASS) {
				compltedCount++;
			}
		}
		for (ImageCard imageCard : imageCards) {
			if (imageCard.getDifficulty() == Difficulty.PASS) {
				compltedCount++;
			}
		}

		StudyLog studyLog = StudyLog.builder()
			.studyDate(LocalDateTime.now())
			.studyCardNumber(remainingCardsCount)
			.studyCardSet(studyCardSet)
			.user(studyCardSet.getUser())
			.build();

		studyLogRepository.save(studyLog);

		Timestamp earliestNextStudyTime = null;
		boolean allCardsPassed = true;

		for (Card card : cards) {
			if (card.getDifficulty() == Difficulty.PASS) {
				continue;
			}
			System.out.println("card = " + card.getCardId());
			allCardsPassed = false;

			Timestamp nextStudyTime = calculateNextStudyTime(card);
			if (nextStudyTime != null) {  // null 체크 추가
				card.setLearnNextTime(nextStudyTime);
				card.setLearnLastTime(Timestamp.valueOf(LocalDateTime.now()));
				cardModuleService.saveCard(card);

				if (earliestNextStudyTime == null || nextStudyTime.before(earliestNextStudyTime)) {
					earliestNextStudyTime = nextStudyTime;
				}
			}
		}

		for (ImageCard imageCard : imageCards) {
			if (imageCard.getDifficulty() == Difficulty.PASS) {
				continue;
			}
			allCardsPassed = false;
			System.out.println("imageCard = " + imageCard.getId());
			Timestamp nextStudyTime = calculateNextStudyTime(imageCard);

			if (nextStudyTime != null) {  // null 체크 추가
				imageCard.setLearnNextTime(nextStudyTime);
				imageCard.setLearnLastTime(Timestamp.valueOf(LocalDateTime.now()));
				cardModuleService.saveImageCard(imageCard);

				if (earliestNextStudyTime == null || nextStudyTime.before(earliestNextStudyTime)) {
					earliestNextStudyTime = nextStudyTime;
				}
			}

		}

		studyCardSet.setRecentStudyDate(LocalDateTime.now());

		studyCardSet.setStudyStatus(StudyStatus.IN_PROGRESS);

		if (allCardsPassed) {
			studyCardSet.setStudyStatus(StudyStatus.PERMANENT_STORAGE);
		}

		if (earliestNextStudyTime != null) {
			studyCardSet.setNextStudyDate(earliestNextStudyTime.toLocalDateTime());
		}

		// 홈 화면 진도율 관련 - 카드셋 완료카드, 학습 예정 카드 업데이트
		studyCardSet.setCardsDueForStudy(remainingCardsCount);
		studyCardSet.setCompletedCardsCount(compltedCount);

		studyCardSetRepository.save(studyCardSet);
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

	public CardResponse.weeklyResultDTO getCardByWeek(String token) {
		Long userId = findUserId(token);
		User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException(INVALID_USERID));

		LocalDate today = LocalDate.now();
		LocalDate startOfWeek = today.with(DayOfWeek.MONDAY); //DayOfWeek.of(1)
		LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

		LocalDate startOfLastWeek = startOfWeek.minusWeeks(1);
		LocalDate endOfLastWeek = endOfWeek.minusWeeks(1);

		// 이번주 학습카드 목록
		List<Card> thisWeekCards = cardRepository.findCardsByUserAndLearnLastTimeBetween(user,
			startOfWeek.atStartOfDay(), endOfWeek.atTime(LocalTime.MAX));
		// 지난주 학습 카드 목록
		List<Card> lastWeekCards = cardRepository.findCardsByUserAndLearnLastTimeBetween(user,
			startOfLastWeek.atStartOfDay(), endOfLastWeek.atTime(LocalTime.MAX));

		// 이번주 일마다 학습한 카드
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

	// 하루 학습한 카드 개수
	private Map<Integer, Long> calculateDailyStudyCount(List<Card> cards) {
		return cards.stream()
			.collect(Collectors.groupingBy(card -> card.getLearnLastTime().toLocalDateTime().getDayOfWeek().getValue(),
				Collectors.collectingAndThen(
					Collectors.toMap(
							card -> card.getStudyCardSet(),
							card -> card.getLearnLastTime(),
							(time1, time2) -> time1.before(time2) ? time1 : time2),
						map -> (long)map.size())));
	}

	// 주간 학습한 카드 개수
	private Map<Integer, Long> initializeWeekStudyResult(Map<Integer, Long> dailyStudyCount) {
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
}

