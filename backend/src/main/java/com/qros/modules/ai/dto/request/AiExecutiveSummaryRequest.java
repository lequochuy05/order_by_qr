package com.qros.modules.ai.dto.request;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record AiExecutiveSummaryRequest(
        LocalDate from,
        LocalDate to,

        @Pattern(regexp = "^(vi|en)$", message = "Language must be vi or en")
        String language) {}
