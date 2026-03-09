package com.emoque.dto;

import jakarta.validation.constraints.NotEmpty;

public record ChatImportRequest(
        @NotEmpty String userId,
        @NotEmpty String openAiApiKey,
        @NotEmpty String conversationId
) {
}
