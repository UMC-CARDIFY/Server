package com.umc.cardify.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.umc.cardify.domain.Card;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.StudyCardSet;
import com.umc.cardify.domain.User;
import com.umc.cardify.domain.enums.CardType;
import com.umc.cardify.domain.enums.StudyStatus;
import com.umc.cardify.repository.CardRepository;
import com.umc.cardify.repository.StudyCardSetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardModuleService {
	private final CardRepository cardRepository;
	private final StudyCardSetRepository studyCardSetRepository;

	public Card saveCard(Card card) {
		return cardRepository.save(card);
	}

	public StudyCardSet findStudyCardSetByNote(Note note) {
		return studyCardSetRepository.findByNote(note).orElseGet(() -> createNewStudyCardSet(note));
	}

	public void addCardToStudyCardSet(Card card, Note note) {
		StudyCardSet studyCardSet = studyCardSetRepository.findByNote(note)
			.orElseGet(() -> createNewStudyCardSet(note));

		// 기존의 StudyCardSet이 있으면 새로 생성하지 않고 연관만 설정
		card.setStudyCardSet(studyCardSet);

		studyCardSetRepository.save(studyCardSet);
	}

	public void processCardNode(Node node, StringBuilder input, Note note) {
		String answer = String.join(" ", node.getAttrs().getAnswer());
		String questionFront = node.getAttrs().getQuestion_front();
		String questionBack = node.getAttrs().getQuestion_back();

		if (questionFront == null)
			questionFront = "";
		if (questionBack == null)
			questionBack = "";

		String nodeText = questionFront + answer + questionBack;
		if (!nodeText.endsWith("."))
			nodeText += ".";
		input.append(nodeText);

		Card card = null;
		switch (node.getType()) {
			case "blank_card" -> {
				card = createCard(note, questionFront, questionBack, answer, CardType.BLANK);
			}
			case "multi_card" -> {
				card = createCard(note, questionFront, null, answer, CardType.MULTI);
			}
			case "word_card" -> {
				card = createCard(note, questionFront, null, answer, CardType.WORD);
			}
		}

		if (card != null) {
			cardRepository.save(card);
			addCardToStudyCardSet(card, note);
		}
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
			.recentStudyDate(null)
			.nextStudyDate(null)
			.build());
	}

	public boolean isCardNode(Node node) {
		return node.getType().equals("word_card") || node.getType().equals("blank_card") || node.getType()
			.equals("multi_card");
	}

	public Card createCard(Note note, String questionFront, String questionBack, String answer, CardType cardType) {
		return Card.builder()
			.note(note)
			.contentsFront(questionFront)
			.contentsBack(questionBack)
			.answer(answer)
			.isLearn(false)
			.countLearn(0L)
			.type(cardType.getValue())  // 카드 타입에 따라 저장
			.build();
	}

	public void saveStudyCardSet(StudyCardSet studyCardSet) {
		studyCardSetRepository.save(studyCardSet);
	}

	public Page<StudyCardSet> getStudyCardSetsByUser(Long userId, Pageable pageable) {
		return studyCardSetRepository.findByUserUserId(userId, pageable);
	}
}
