package com.umc.cardify.service;

import static com.umc.cardify.config.exception.ErrorResponseStatus.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.DatabaseException;
import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.ImageCard;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.Overlay;
import com.umc.cardify.domain.ProseMirror.Attr;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.CardType;
import com.umc.cardify.domain.enums.StudyStatus;
import com.umc.cardify.dto.card.CardRequest;
import com.umc.cardify.repository.CardRepository;
import com.umc.cardify.repository.ImageCardRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.OverlayRepository;
import com.umc.cardify.repository.StudyCardSetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardModuleService {

	private final CardRepository cardRepository;
	private final StudyCardSetRepository studyCardSetRepository;
	private final ImageCardRepository imageCardRepository;
	private final OverlayRepository overlayRepository;
	private final NoteRepository noteRepository;
	private final S3Service s3Service;
    private final ObjectMapper objectMapper;

	// note : 카드 타입 조회
	public int getCardType(Long cardId) {
		int cardType = cardRepository.findCardTypeByCardId(cardId);
		return cardType;
	}

	// note : 카드 난이도 조회
	public int getCardDifficulty(Long cardId, int cardType) {
		if (cardType == 0) {
			return cardRepository.findCardDifficultyByCardId(cardId);
		} else {
			return imageCardRepository.findImageCardDifficultyByCardId(cardId);
		}
	}

	boolean existsByNote(Note note){

		return studyCardSetRepository.existsByNote(note);
	}
	public void deleteCardSet(Long studyCardSetId) {

		studyCardSetRepository.deleteById(studyCardSetId);
	}

	void deleteAllCardsByNoteId(Long noteId) {
		cardRepository.deleteCardsByNoteId(noteId);
	}

	void deleteAllImageCardsByNoteId(Long noteId) {
		Note note = noteRepository.findById(noteId).orElseThrow(() -> new DatabaseException(NOT_FOUND_ERROR));
		StudyCardSet studyCardSet = studyCardSetRepository.findByNote(note)
			.orElseThrow(() -> new DatabaseException(NOT_FOUND_ERROR));
		imageCardRepository.deleteAllByStudyCardSet(studyCardSet);
	}

	public List<Card> findAllCardsByUserIdAndLearnNextTimeOnDate(Long userId, Timestamp date) {
		return cardRepository.findAllByUserIdAndLearnNextTimeOnDate(userId, date);
	}
	public List<ImageCard> findAllImageCardsByUserIdAndLearnNextTimeOnDate(Long userId, Timestamp date) {
		return imageCardRepository.findAllByUserIdAndLearnNextTimeOnDate(userId, date);
	}

	// 카드 노드 처리
	public void processCardNode(Node node, StringBuilder input, Note note, Queue<MultipartFile> imageQueue) {
		if (isImageCard(node)) {
			processImageCard(node, note, imageQueue);
		} else {
			processTextCard(node, input, note);
		}
	}

	private boolean isImageCard(Node node) {
		return node.getType().equals("image_card");
	}

	private void processImageCard(Node node, Note note, Queue<MultipartFile> imageQueue) {
		MultipartFile image = imageQueue.poll();
		if (image == null) {
			throw new BadRequestException(NOT_FOUND_IMAGE);
		}

		String imgUrl = s3Service.upload(image, "imageCards");
		ImageCard imageCard = buildImageCard(imgUrl, node.getAttrs());
		imageCard.setStudyCardSet(findStudyCardSetByNote(note));

		ImageCard savedImageCard = imageCardRepository.save(imageCard);
		saveOverlays(node.getAttrs().getOverlays(), savedImageCard);
	}

	// NOTE : 이미지 카드 최초 생성 시 다음 학습시간은 즉시 학습으로 지정
	private ImageCard buildImageCard(String imgUrl, Attr attrs) {
		return ImageCard.builder()
			.imageUrl(imgUrl)
			.height(attrs.getBaseImageHeight())
			.width(attrs.getBaseImageWidth())
			.countLearn(0L)
			.learnNextTime(Timestamp.valueOf(LocalDateTime.now())) // 즉시 학습
			.build();
	}

	private void saveOverlays(List<CardRequest.addImageCardOverlay> overlayRequests, ImageCard imageCard) {
		if (overlayRequests != null) {
			overlayRequests.forEach(overlayRequest -> {
				Overlay overlay = buildOverlay(overlayRequest, imageCard);
				overlayRepository.save(overlay);
			});
		}
	}

	private Overlay buildOverlay(CardRequest.addImageCardOverlay overlayRequest, ImageCard imageCard) {
		return Overlay.builder()
			.xPosition(overlayRequest.getPositionOfX())
			.yPosition(overlayRequest.getPositionOfY())
			.width(overlayRequest.getWidth())
			.height(overlayRequest.getHeight())
			.imageCard(imageCard)
			.build();
	}

	private void processTextCard(Node node, StringBuilder input, Note note) {
		String questionFront = getOrDefault(node.getAttrs().getQuestion_front(), "");
		String questionBack = getOrDefault(node.getAttrs().getQuestion_back(), "");
		String answer = String.join(", ", node.getAttrs().getAnswer());

		String nodeText = buildNodeText(questionFront, answer, questionBack);
		input.append(nodeText);

        String content;
        try {
            content = objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e){
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }

            Card card = createCardBasedOnType(node.getType(), note, content, questionFront, questionBack, answer);
		if (card != null) {
			cardRepository.save(card);
			addCardToStudyCardSet(card, note);
		}
	}

	private String buildNodeText(String questionFront, String answer, String questionBack) {
		String nodeText = questionFront + answer + questionBack;
		if (!nodeText.endsWith(".")) {
			nodeText += ".";
		}
		return nodeText;
	}

	private String getOrDefault(String value, String defaultValue) {
		return value != null ? value : defaultValue;
	}

	private Card createCardBasedOnType(String type, Note note, String contents, String questionFront, String questionBack,
		String answer) {
		switch (type) {
            case "blank_card":
            case "blankcard":
				return createCard(note, contents, questionFront, questionBack, answer, CardType.BLANK);
			case "multi_card":
            case "multicard":
				return createCard(note, contents, questionFront, null, answer, CardType.MULTI);
			case "word_card":
            case "wordcard":
				return createCard(note, contents, questionFront, null, answer, CardType.WORD);
			default:
				return null;
		}
	}

	// NOTE : 카드 생성 시, 즉시 '카드 학습 시간' 생성
	public Card createCard(Note note, String contents, String questionFront, String questionBack, String answer, CardType cardType) {
		return Card.builder()
			.note(note)
            .contents(contents)
			.contentsFront(questionFront)
			.contentsBack(questionBack)
			.answer(answer)
			.countLearn(0L)
			.learnNextTime(Timestamp.valueOf(LocalDateTime.now())) // 생성된 시간으로 다음 학습 시간이 생성됨
			.type(cardType.getValue())
			.build();
	}

	// StudyCardSet 관련 메서드
	public StudyCardSet findStudyCardSetByNote(Note note) {
		return studyCardSetRepository.findByNote(note).orElseGet(() -> createNewStudyCardSet(note));
	}

	public StudyCardSet createNewStudyCardSet(Note note) {
		Folder folder = note.getFolder();
		User user = folder.getUser();
		return studyCardSetRepository.save(StudyCardSet.builder()
			.note(note)
			.folder(folder)
			.user(user)
			.studyStatus(StudyStatus.BEFORE_STUDY)
			.noteName(note.getName())
			.color(folder.getColor())
			.build());
	}

	public void addCardToStudyCardSet(Card card, Note note) {
		StudyCardSet studyCardSet = findStudyCardSetByNote(note);
		card.setStudyCardSet(studyCardSet);
		studyCardSetRepository.save(studyCardSet);
	}

	public void saveStudyCardSet(StudyCardSet studyCardSet) {
		studyCardSetRepository.save(studyCardSet);
	}

	// 기타 메서드
	public List<StudyCardSet> getStudyCardSetsByUser(Long userId) {
		return studyCardSetRepository.findByUserUserId(userId);
	}

	public List<Card> getCardsByStudyCardSet(StudyCardSet studyCardSet) {
		return cardRepository.findByStudyCardSet(studyCardSet);
	}

	public StudyCardSet getStudyCardSetById(Long id) {
		return studyCardSetRepository.findById(id).orElseThrow(() -> new DatabaseException(NOT_FOUND_ERROR));
	}

	public List<ImageCard> getImageCardsByStudyCardSet(StudyCardSet studyCardSet) {
		return imageCardRepository.findByStudyCardSet(studyCardSet);
	}

	public Card getCardById(Long id) {
		return cardRepository.findById(id).orElseThrow(() -> new DatabaseException(NOT_FOUND_ERROR));
	}

	public ImageCard getImageCardById(Long imageCardId) {
		return imageCardRepository.findById(imageCardId).orElseThrow(() -> new DatabaseException((NOT_FOUND_ERROR)));
	}

	public void updateWordCardDifficulty(Card card) {
		cardRepository.save(card);
	}

	public void updateImageCardDifficulty(ImageCard imageCard) {
		imageCardRepository.save(imageCard);
	}

	public void deleteAllCards(List<Card> cards) {
		cardRepository.deleteAll(cards);
	}

	public void deleteAllImageCards(List<ImageCard> imageCards) {
		imageCardRepository.deleteAll(imageCards);
	}

	public void saveCard(Card card) {
		cardRepository.save(card);
	}

	public void saveImageCard(ImageCard imageCard) {
		imageCardRepository.save(imageCard);
	}

	// note : 카드 난이도에 따른 학습 시간 반환 메소드에 사용됨
	public int getCardCountLearn(Long cardId, int cardType) {
		if (cardType == 0) {
			Card card = getCardById(cardId);
			return card.getCountLearn() == null ? 0 : card.getCountLearn().intValue();
		} else {
			ImageCard imageCard = getImageCardById(cardId);
			return imageCard.getCountLearn() == null ? 0 : imageCard.getCountLearn().intValue();
		}
	}
}

