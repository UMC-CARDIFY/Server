package com.umc.cardify.service;

import com.umc.cardify.config.exception.BadRequestException;
import com.umc.cardify.config.exception.ErrorResponseStatus;
import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.ProseMirror.Node;
import com.umc.cardify.domain.User;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoteFacadeService {
    private final CardModuleService cardModuleService;
    private final NoteService noteService;

    @Transactional
    public boolean writeNoteFacade(User user, String mode, Long noteId, String name, Node node, List<MultipartFile> images){
        Note note = noteService.getNoteByIdWithLock(noteId);

        noteService.checkOwnership(user, note);

        if(mode == null || mode.isEmpty())
            mode = "standard";
        if(!mode.equals("standard") && !mode.equals("light"))
            throw new BadRequestException(ErrorResponseStatus.REQUEST_ERROR);

        if (cardModuleService.existsByNote(note) && mode.equals("standard")) {
            cardModuleService.deleteAllCardsByNoteId(note);
            cardModuleService.deleteAllImageCardsByNoteId(note);
        }

        note.setName(name);

        if(mode.equals("standard")) {
            StringBuilder totalText = new StringBuilder();

            Queue<MultipartFile> imageQueue = new LinkedList<>(images != null ? images : Collections.emptyList());
            parsingNode(node, totalText, note, imageQueue);
            note.setTotalText(totalText.toString());
        }

       return noteService.writeNote(note, node, images);
    }

    public void parsingNode(Node node, StringBuilder input, Note note, Queue<MultipartFile> imageQueue) {
        if (node.getType().endsWith("card")) {
            cardModuleService.processCardNode(node, input, note, imageQueue);
        } else if (node.getType().equals("text")) {
            processTextNode(node, input);
        }

        if (node.getContent() != null) {
            node.getContent().forEach(content -> parsingNode(content, input, note, imageQueue));
        }
    }

    private void processTextNode(Node node, StringBuilder input) {
        String nodeText = node.getText();
        if (!nodeText.endsWith("."))
            nodeText += ".";
        input.append(nodeText);
    }
}
