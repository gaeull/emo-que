package com.emoque.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GenerationRequest(
        @NotEmpty String userId,
        @NotEmpty List<String> emotions
) {
}
