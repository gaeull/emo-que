package com.emoque.dto;

public record IntroTaskResponse(
        String taskId,
        String status,
        String intro,
        String failureReason
) {
}
