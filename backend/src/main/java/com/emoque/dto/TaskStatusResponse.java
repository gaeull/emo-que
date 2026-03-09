package com.emoque.dto;

public record TaskStatusResponse(
        String taskId,
        String status,
        String failureReason
) {
}
