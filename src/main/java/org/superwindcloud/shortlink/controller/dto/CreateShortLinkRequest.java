package org.superwindcloud.shortlink.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortLinkRequest(
    @NotBlank(message = "URL is required") @Size(max = 2048) String url) {}
