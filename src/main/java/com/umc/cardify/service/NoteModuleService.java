package com.umc.cardify.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.converter.NoteConverter;
import com.umc.cardify.domain.Folder;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.User;
import com.umc.cardify.repository.LibraryRepository;
import com.umc.cardify.repository.NoteRepository;
import com.umc.cardify.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoteModuleService {
	private final NoteRepository noteRepository;
	private final UserRepository userRepository;
	private final LibraryRepository libraryRepository;

	public Note getNoteById(long noteId) {
		return noteRepository.findById(noteId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.NOT_FOUND_ERROR));
	}

	public Note addNote(Folder folder, Long userId) {
		if (!userId.equals(folder.getUser().getUserId()))
			throw new BadRequestException(ErrorResponseStatus.INVALID_USERID);
		else {
			Note newNote = NoteConverter.toAddNote(folder);
			return noteRepository.save(newNote);
		}
	}
	public void saveNote(Note note) {
		noteRepository.save(note);
	}

	public void deleteNoteById(Long noteId) {
		Note note = getNoteById(noteId);
		noteRepository.delete(note);
	}

	public Page<Note> getNotesByUser(Long userId, Pageable pageable) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
		return noteRepository.findByUser(user, pageable);
	}

	public boolean isNoteInLibrary(Note note) {
		return libraryRepository.findByNote(note) != null;
	}

	public User getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BadRequestException(ErrorResponseStatus.INVALID_USERID));
	}
}
