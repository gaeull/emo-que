package com.emoque.dto;

import jakarta.validation.constraints.NotEmpty;

public record GoogleLoginRequest(@NotEmpty String idToken) {}

