package com.emoque.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SurveyRequest(
        @NotEmpty String name,
        @Email @NotEmpty String email,
        @NotEmpty String gender,
        @NotEmpty String job,
        @Size(min = 2, max = 4) String mbti,
        @NotNull @Size(min = 1, max = 5) List<String> personalityKeywords,
        List<String> sampleEmoticonUrls
) {
}
