package com.umc.cardify.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.umc.cardify.domain.*;
import com.umc.cardify.domain.enums.SubscriptionStatus;
import com.umc.cardify.dto.note.NoteComparator;
import com.umc.cardify.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.enums.MarkStatus;
import com.umc.cardify.dto.note.NoteRequest;
import com.umc.cardify.dto.note.NoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteComponentService {
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final LibraryRepository libraryRepository;
	private final CategoryRepository categoryRepository;
	private final LibraryCategoryRepository libraryCategoryRepository;
	private final ContentsNoteRepository contentsNoteRepository;
	private final FolderRepository folderRepository;
	private final SearchHistoryRepository searchHistoryRepository;

	private final NoteModuleService noteModuleService;
	private final CardModuleService cardModuleService;

	private final NoteConverter noteConverter;
	private final ObjectMapper objectMapper;

	private static final int PREVIEW_LIMIT = 300;


	public Note addNote(Folder folder, Long userId) {
		if (!userId.equals(folder.getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			Note newNote = NoteConverter.toAddNote(folder);
			noteRepository.save(newNote);
			try {
				ContentsNote contentsNote = ContentsNote.builder().note(newNote).build();
				contentsNoteRepository.save(contentsNote);
				newNote.setContentsNote(contentsNote);
			}catch (Exception e){
				System.out.println(e);
			}

			noteRepository.save(newNote);
			return newNote;
		}
	}

	public Boolean checkNoteCnt(Long userId){
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));

		if(user.getSubscriptions().stream()
				.anyMatch(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE))
			return true;

		int note_cnt = 0;
		List<Integer> cnt_list = folderRepository.findByUser(user).stream()
				.map(folder -> folder.getNotes().size())
				.toList();
        for (Integer cnt : cnt_list)
            note_cnt += cnt;

        return note_cnt <= 19;
	}
	public Boolean deleteNote(Long noteId, Long userId) {
		Note note_del = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		if (!userId.equals(note_del.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			noteRepository.delete(note_del);
			contentsNoteRepository.delete(contentsNoteRepository.findByNote(note_del).get());
			return true;
		}
	}

	@Transactional
	public NoteResponse.GetNoteToFolderResultDTO getNoteToFolder(Folder folder,
		NoteRequest.GetNoteToFolderDto request) {
		Pageable pageable;

		Integer page = request.getPage();
		if (page == null)
			page = 0;

		Integer size = request.getSize();
		if (size == null)
			size = folder.getNotes().size();

		String order = request.getOrder();
		if (order == null)
			order = "create-newest";

		pageable = switch (order.toLowerCase()) {
			case "asc" -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("name")));
			case "desc" -> PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("name")));
			case "edit-newest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("editDate")));
			case "edit-oldest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("editDate")));
			case "create-newest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.desc("createdAt")));
			case "create-oldest" ->
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("markAt"), Sort.Order.asc("createdAt")));
			default -> throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
		};
		Page<Note> notes_all = noteRepository.findByFolder(folder, pageable);

		NoteResponse.GetNoteToFolderResultDTO noteDTO = noteConverter.toGetNoteToFolderResult(folder, notes_all);
		return noteDTO;
	}

	public Boolean markNote(Long noteId, Boolean isMark, Long userId) {
		Note note_mark = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		if (!userId.equals(note_mark.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			if (isMark) {
				note_mark.setMarkState(MarkStatus.ACTIVE);
				note_mark.setMarkAt(LocalDateTime.now());
			} else if (!isMark) {
				note_mark.setMarkState(MarkStatus.INACTIVE);
				note_mark.setMarkAt(null);
			} else
				throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
			noteRepository.save(note_mark);
			return true;
		}
	}

	@Transactional
	public Boolean writeNote(NoteRequest.WriteNoteDto request, Long userId, List<MultipartFile> images) {
        // 작성 모드 설정
        String mode = request.getMode();
        if(mode == null || mode.isEmpty())
            mode = "standard";
        if(!mode.equals("standard") && !mode.equals("light"))
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);

		Note note = noteModuleService.getNoteById(request.getNoteId());

		if (!userId.equals(note.getFolder().getUser().getUserId())) {
			log.warn("Invalid userId: {}", userId);
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		}
		if (!note.getIsEdit()) {
			log.warn("IsEdit is : {}", note.getIsEdit());
			throw new BadRequestException(ErrorResponseStatus.DB_UPDATE_ERROR);
		}
		if (cardModuleService.existsByNote(note) && mode.equals("standard")) {
			cardModuleService.deleteAllCardsByNoteId(note.getNoteId());
			cardModuleService.deleteAllImageCardsByNoteId(note.getNoteId());
		}
        note.setName(request.getName());
        Node node = request.getContents();

        if(mode.equals("standard")) {
            StringBuilder totalText = new StringBuilder();

            Queue<MultipartFile> imageQueue = new LinkedList<>(images != null ? images : Collections.emptyList());
            searchCard(node, totalText, note, imageQueue);
            note.setTotalText(totalText.toString());
        }

		ContentsNote contentsNote = contentsNoteRepository.findByNote(note)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));

        String content;
        try {
            content = objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e){
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
        }
		contentsNote.setContents(content);
		contentsNoteRepository.save(contentsNote);

		noteModuleService.saveNote(note);

		return true;
	}

	public void searchCard(Node node, StringBuilder input, Note note, Queue<MultipartFile> imageQueue) {
		if (node.getType().endsWith("card")) {
			cardModuleService.processCardNode(node, input, note, imageQueue);
		} else if (node.getType().equals("text")) {
			processTextNode(node, input);
		}

		if (node.getContent() != null) {
			node.getContent().forEach(content -> searchCard(content, input, note, imageQueue));
		}
	}

	public void processTextNode(Node node, StringBuilder input) {
		String nodeText = node.getText();
		if (!nodeText.endsWith("."))
			nodeText += ".";
		input.append(nodeText);
	}

	public List<NoteResponse.SearchNoteResDTO> searchNote(Folder folder, String search) {
		//문단 구분점인 .을 입력시 빈 리스트 반환
		if (search.trim().equals("."))
			return null;

		List<Note> notes = noteRepository.findByFolder(folder)
			.stream()
			.filter(note -> note.getName().contains(search) | note.getTotalText().contains(search))
			.toList();
		List<NoteResponse.SearchNoteResDTO> searchList = notes.stream()
			.map(list -> noteConverter.toSearchNoteResult(list, search))
			.collect(Collectors.toList());

		return searchList;
	}

	public NoteResponse.SearchNoteAllDTO searchNoteAll(User user, String search) {
		//문단 구분점인 .을 입력시 빈 리스트 반환
		if (search.trim().equals("."))
			return null;

		//User가 갖고 있는 Folder 조회
		List<Folder> folderList = folderRepository.findByUser(user);
		//Folder내 검색어가 포함된 노트 조회
		List<NoteResponse.SearchNoteToUserDTO> noteToUserDTO = new ArrayList<>(folderList.stream()
                .map(folder -> {
                    List<NoteResponse.SearchNoteResDTO> folderToNote = noteRepository.findByFolder(folder).stream()
                            .filter(note -> note.getName().contains(search) | note.getTotalText().contains(search))
                            .map(note -> noteConverter.toSearchNoteResult(note, search))
                            .toList();
                    if (!folderToNote.isEmpty())
                        return noteConverter.toSearchNoteUser(folder, folderToNote);
                    return null;
                }).toList());
		noteToUserDTO.remove(null);
		//Library내 검색어가 포함된 노트 조회
		List<NoteResponse.SearchNoteToLibDTO> noteToLibDTO = libraryRepository.findAll().stream()
				.filter(library -> library.getNote().getName().contains(search) | library.getNote().getTotalText().contains(search))
				.map(library -> NoteResponse.SearchNoteToLibDTO.builder()
                        .libraryId(library.getLibraryId())
						.note(noteConverter.toSearchNoteResult(library.getNote(), search))
                        .build())
				.toList();

		return NoteResponse.SearchNoteAllDTO.builder()
				.searchTxt(search).noteToUserList(noteToUserDTO).noteToLibList(noteToLibDTO)
				.build();
	}

	public void addSearchHistory(User user, String search){
		SearchHistory searchHistory = searchHistoryRepository.findFirstByUserAndSearch(user, search);
		if(searchHistory != null){
			//set history
			searchHistory.setSearchAt(LocalDateTime.now());
			searchHistoryRepository.save(searchHistory);
			return ;
		}

		//last history del
		List<SearchHistory> searchHistoryList = searchHistoryRepository.findAllByUser(user)
				.stream().sorted(Comparator.comparing(SearchHistory::getSearchAt).reversed()).toList();

		int list_size = searchHistoryList.size();
		if(list_size >= 5 ){
			searchHistoryRepository.delete(searchHistoryList.get(list_size - 1));
		}

		//add new history
		SearchHistory history_input = SearchHistory.builder()
				.user(user)
				.search(search)
				.searchAt(LocalDateTime.now())
				.build();
		searchHistoryRepository.save(history_input);
	}

	public List<String> getSearchHistory(User user){
		return searchHistoryRepository.findAllByUser(user).stream()
				.sorted(Comparator.comparing(SearchHistory::getSearchAt).reversed())
				.map(SearchHistory::getSearch)
				.toList();
	}

	public Boolean delSearchHistory(User user, String search){
		SearchHistory searchHistory = searchHistoryRepository.findFirstByUserAndSearch(user, search);
		if(searchHistory == null)
			throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR);

		searchHistoryRepository.delete(searchHistory);

		return true;
	}

	public Boolean shareLib(Long userId, NoteRequest.ShareLibDto request) {
		Note note = noteModuleService.getNoteById(request.getNoteId());
		if (!userId.equals(note.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		if (note.getDownloadLibId() != null)
			throw new BadRequestException(ErrorResponseStatus.DB_INSERT_ERROR);
		//기존에 공유되어 있던 데이터를 삭제
		Library library = note.getLibrary();
		if (library != null) {
			note.setLibrary(null);
			libraryRepository.delete(library);
		}

		Library library_new = Library.builder().note(note).uploadAt(LocalDateTime.now()).build();
		libraryRepository.save(library_new);

		List<Category> categoryList = null;
		if (request.getCategory().size() > 0 && request.getCategory().size() <= 3) {
			categoryList = request.getCategory().stream().map(categoryStr -> {
				Category category = categoryRepository.findByName(categoryStr);
				if (category == null)
					throw new BadRequestException(ErrorResponseStatus.NOT_FOUND_CATEGORY);
				return category;
			}).toList();
		} else
			throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
		if (categoryList != null) {
			categoryList.stream()
				.map(category -> libraryCategoryRepository.save(
					LibraryCategory.builder().category(category).library(library_new).build()))
				.toList();
		}

		note.setIsEdit(false);
		noteRepository.save(note);
		return true;
	}

	public Boolean cancelShare(Long noteId, Long userId) {
		Note note = noteModuleService.getNoteById(noteId);
		if (!userId.equals(note.getFolder().getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		Library library = note.getLibrary();

		note.setLibrary(null);
		note.setIsEdit(true);
		noteRepository.save(note);

		if (library != null)
			libraryRepository.delete(library);
		return true;
	}

	public NoteResponse.getNoteDTO getNote(Long noteId) {
        Note note = noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		//노트 조회 시간 갱신
		note.setViewAt(LocalDateTime.now());
		noteRepository.save(note);

		//노트 내용 반환
		List<NoteResponse.getNoteCardDTO> cardDTO = note.getCards().stream().map(card -> {
			return NoteResponse.getNoteCardDTO.builder()
				.cardId(card.getCardId())
				.cardName(note.getName())
                .contents(card.getContents())
				.contentsFront(card.getContentsFront())
				.contentsBack(card.getContentsBack())
				.build();
		}).toList();

        return noteConverter.getNoteDTO(note, cardDTO);
	}

	public List<NoteResponse.NoteInfoDTO> getRecentNotes(Long userId, int page, Integer size) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
		int recentNoteSize = (size != null) ? size : 5;
		Pageable pageable = PageRequest.of(page, recentNoteSize);
		Page<Note> notes = noteRepository.findByUserOrderByViewAtDesc(user, pageable);
		return notes.stream().map(noteConverter::recentNoteInfoDTO).collect(Collectors.toList());
	}

	// 노트 조회, 정렬, 필터링 통합 Service
	public NoteResponse.NoteListDTO getNotesBySortFilter(Long userId, Integer page, Integer size, String order, String filter, Long folderId) {
		try {
			// 1. 사용자 검증
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));

		// 폴더 존재 여부 확인
		Folder folder = folderRepository.findById(folderId)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID));

			// 3. 폴더 소유권 확인
			if (!folder.getUser().getUserId().equals(userId)) {
				throw new BadRequestException(ErrorResponseStatus.INVALID_FOLDERID);
			}

			// 4. 페이징 설정
			int getNotePage = (page != null) ? page : 0;
			int getNoteSize = (size != null) ? size : Integer.MAX_VALUE;

			// 5. 모든 노트 조회 (기본 북마크 + 수정일 최신순 정렬)
			Sort defaultSort = Sort.by(Sort.Order.desc("markState"))
					.and(Sort.by(Sort.Order.desc("editDate")));
			List<Note> allNotes = noteRepository.findByFolder(folder, defaultSort);

			if (allNotes.isEmpty()) {
				return noteConverter.createEmptyNoteListDTO(getNotePage, getNoteSize);
			}

			// 6. 카드 개수 정보 조회 (필터링에 필요)
			Map<Long, Long> cardCountMap = getCardCountMap(folder);

			// 7. 필터 적용
			List<Note> filteredNotes = filterNotesByCardCount(allNotes, filter, cardCountMap);

			// 8. 정렬 적용
			List<Note> sortedNotes = sortNotes(filteredNotes, order);

			// 9. 페이징 적용
			List<Note> pagedNotes = pagingNotes(sortedNotes, getNotePage, getNoteSize);

			// 10. DTO 변환
			List<NoteResponse.NoteInfoDTO> noteInfos = pagedNotes.stream()
					.map(note -> noteConverter.convertToNoteInfoDTO(note, cardCountMap))
					.collect(Collectors.toList());

			log.info("DTO 변환 완료: {} 개의 노트", noteInfos.size());

			// 11. 페이징 정보 계산
			int totalElements = sortedNotes.size();
			int totalPages = (totalElements + getNoteSize - 1) / getNoteSize;

			return NoteResponse.NoteListDTO.builder()
					.noteList(noteInfos)
					.listsize(getNoteSize)
					.currentPage(getNotePage + 1)
					.totalPage(totalPages)
					.totalElements((long) totalElements)
					.isFirst(getNotePage == 0)
					.isLast(getNotePage == totalPages - 1)
					.build();

		} catch (Exception e) {
			log.error("노트 조회 중 오류 발생", e);
			throw e;
		}
	}

	private Map<Long, Long> getCardCountMap(Folder folder) {
		try {
			List<Object[]> cardCounts = noteRepository.findNoteCardCounts(folder);
			return cardCounts.stream()
					.collect(Collectors.toMap(
							arr -> (Long) arr[0],  // noteId
							arr -> (Long) arr[1]   // card count
					));
		} catch (Exception e) {
			log.warn("카드 개수 조회 실패, 기본값 사용", e);
			return new HashMap<>();
		}
	}

	// 카드 개수별 필터링
	private List<Note> filterNotesByCardCount(List<Note> notes, String filter, Map<Long, Long> cardCountMap) {
		if (filter == null || filter.isEmpty()) {
			return notes;
		}

		switch (filter) {
			case "card-most":
				// 카드가 1개 이상인 노트만 반환
				return notes.stream()
						.filter(note -> cardCountMap.getOrDefault(note.getNoteId(), 0L) >= 1)
						.collect(Collectors.toList());

			case "card-less":
				// 카드가 0개인 노트만 반환
				return notes.stream()
						.filter(note -> cardCountMap.getOrDefault(note.getNoteId(), 0L) == 0)
						.collect(Collectors.toList());

			default:
				throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);
		}
	}

	// 노트 정렬 (북마크 상태 + 조건별 정렬)
	private List<Note> sortNotes(List<Note> notes, String order) {
		return notes.stream()
				.sorted(new NoteComparator(order))
				.collect(Collectors.toList());
	}

	// 페이징 적용
	private List<Note> pagingNotes(List<Note> notes, int page, int size) {
		int start = page * size;
		int end = Math.min((page + 1) * size, notes.size());
		return notes.subList(start, end);
	}

	public NoteResponse.getNoteUUIDDTO createNoteUUID(NoteRequest.MakeLinkDto request, User user){
		Note note = noteModuleService.getNoteById(request.getNoteId());
		if (!user.equals(note.getFolder().getUser()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);

		if(note.getUuid() != null){
			return NoteResponse.getNoteUUIDDTO.builder()
					.noteId(note.getNoteId())
					.UUID(note.getUuid())
					.build();
		}

		note.setUuid(UUID.randomUUID().toString());
		Note result = noteRepository.save(note);

		return NoteResponse.getNoteUUIDDTO.builder()
				.noteId(result.getNoteId())
				.UUID(result.getUuid())
				.build();
	}

	public Long getNoteIdToUUID(String UUID){
		Note note = noteRepository.findByUuid(UUID).orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));

		return note.getNoteId();
	}

	public Boolean delNoteUUID(User user, Long noteId){
		Note note = noteRepository.findById(noteId)
				.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
		if (!user.equals(note.getFolder().getUser()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);

		note.setUuid(null);
		noteRepository.save(note);

		return true;
	}

	public List<NoteResponse.RecentNoteDTO> getRecentFavoriteNotes(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(()-> new BadRequestException(ErrorResponseStatus.REQUEST_ERROR));

		List<Note> notes = noteRepository.findRecentFavoriteNotes(MarkStatus.ACTIVE, user, PageRequest.of(0, 3));

		List<NoteResponse.RecentNoteDTO> result = notes.stream()
				.map(note -> NoteResponse.RecentNoteDTO.builder()
						.noteId(note.getNoteId())
						.name(note.getName())
						.folderId(note.getFolder().getFolderId())
						.folderColor(note.getFolder().getColor())
						.markState(note.getMarkState())
						.markAt(note.getMarkAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
						.noteContentPreview(getNotePreview(note))
						.flashCardCount(note.getCards().size())
						.build())
				.toList();

		return result;
	}

	private String getNotePreview(Note note) {
		String t = (note.getTotalText() == null || ".".equals(note.getTotalText())) ? "" : note.getTotalText();

		if (t.isBlank()) { return null; }

		String normalized = t.replaceAll("\\s+", " ").trim();
		return ellipsize(normalized, PREVIEW_LIMIT);
	}

	private String ellipsize(String s, int maxCodePoints) {
		if (s == null) return "";
		s = s.strip();
		if (s.isEmpty()) return s;
		int lengthCp = s.codePointCount(0, s.length());
		if (lengthCp <= maxCodePoints) return s;
		int endIdx = s.offsetByCodePoints(0, maxCodePoints);
		return s.substring(0, endIdx).stripTrailing() + "...";
	}

    public List<NoteResponse.getNoteCardDTO> getNoteCards(User user, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
        if (!user.equals(note.getFolder().getUser()))
            throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);

        return note.getCards().stream().map(card -> NoteResponse.getNoteCardDTO.builder()
                .cardId(card.getCardId())
                .cardName(note.getName())
                .contents(card.getContents())
                .contentsFront(card.getContentsFront())
                .contentsBack(card.getContentsBack())
                .build()).toList();
    }
}