package com.emoque.dto;

import java.util.List;
import java.util.Map;

public record GenerationResponse(
        String taskId,
        String status,
        String bio,
        Map<String, String> emotionImageUrls,
        List<String> downloadLinks
) {
}
