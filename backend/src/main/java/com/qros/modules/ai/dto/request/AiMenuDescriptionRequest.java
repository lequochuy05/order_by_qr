package com.qros.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiMenuDescriptionRequest(
        @NotBlank @Size(max = 200) String itemName,
        @Size(max = 200) String categoryName,
        @Size(max = 50) String price,
        List<String> ingredients) {}
