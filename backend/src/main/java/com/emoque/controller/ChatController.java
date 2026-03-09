package com.emoque.controller;

import com.emoque.dto.ChatImportRequest;
import com.emoque.dto.TaskStatusResponse;
import com.emoque.model.IntroTask;
import com.emoque.service.ChatImportService;
import com.emoque.service.IntroTaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatImportService chatImportService;
    private final IntroTaskService introTaskService;

    public ChatController(ChatImportService chatImportService, IntroTaskService introTaskService) {
        this.chatImportService = chatImportService;
        this.introTaskService = introTaskService;
    }

    @PostMapping("/import")
    public ResponseEntity<Void> importChat(@Valid @RequestBody ChatImportRequest request) {
        chatImportService.importConversation(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(path = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<TaskStatusResponse> uploadChat(@RequestPart("file") MultipartFile file,
                                                         @RequestParam("userId") String userId) {
        chatImportService.importConversationFile(userId, file);
        IntroTask task = introTaskService.enqueue(userId);
        return ResponseEntity.accepted()
                .body(new TaskStatusResponse(task.getId(), task.getStatus().name(), task.getFailureReason()));
    }
}
