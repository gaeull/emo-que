package com.emoque.controller;

import com.emoque.dto.BioRequest;
import com.emoque.dto.IntroTaskResponse;
import com.emoque.dto.TaskStatusResponse;
import com.emoque.model.IntroTask;
import com.emoque.service.IntroTaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bio")
public class BioController {

    private final IntroTaskService introTaskService;

    public BioController(IntroTaskService introTaskService) {
        this.introTaskService = introTaskService;
    }

    @PostMapping
    public ResponseEntity<TaskStatusResponse> enqueueIntro(@Valid @RequestBody BioRequest request) {
        IntroTask task = introTaskService.enqueue(request.userId());
        return ResponseEntity.accepted()
                .body(new TaskStatusResponse(task.getId(), task.getStatus().name(), task.getFailureReason()));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<IntroTaskResponse> getIntro(@PathVariable String taskId) {
        IntroTask task = introTaskService.getTask(taskId);
        return ResponseEntity.ok(new IntroTaskResponse(
                task.getId(),
                task.getStatus().name(),
                task.getIntro(),
                task.getFailureReason()));
    }
}
