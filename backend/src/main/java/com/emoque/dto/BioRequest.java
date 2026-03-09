package com.emoque.dto;

import jakarta.validation.constraints.NotEmpty;

public record BioRequest(@NotEmpty String userId) {}

