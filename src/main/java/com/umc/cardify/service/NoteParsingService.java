package com.umc.cardify.service;

import com.umc.cardify.domain.Note;
import com.umc.cardify.domain.ProseMirror.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Queue;

@Service
@RequiredArgsConstructor
public class NoteParsingService {
    private final CardModuleService cardModuleService;

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

    public void processTextNode(Node node, StringBuilder input) {
        String nodeText = node.getText();
        if (!nodeText.endsWith("."))
            nodeText += ".";
        input.append(nodeText);
    }
}
