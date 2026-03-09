package com.emoque.controller;

import com.emoque.dto.GenerationRequest;
import com.emoque.dto.GenerationResponse;
import com.emoque.dto.TaskStatusResponse;
import com.emoque.dto.DownloadZipRequest;
import com.emoque.service.GenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generation")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public ResponseEntity<TaskStatusResponse> createTask(@Valid @RequestBody GenerationRequest request) {
        return ResponseEntity.accepted().body(generationService.enqueueGeneration(request));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<GenerationResponse> getTask(@PathVariable String taskId) {
        return ResponseEntity.ok(generationService.getTask(taskId));
    }

    @PostMapping("/{taskId}/download")
    public ResponseEntity<byte[]> downloadPack(@PathVariable String taskId,
                                               @RequestBody(required = false) DownloadZipRequest request) {
        byte[] zip = generationService.buildDownloadZip(taskId, request != null ? request.images() : null);
        if (zip.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"emoticons-" + taskId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}
